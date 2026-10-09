package com.petcare.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.petcare.app.data.remote.FirestoreCollections
import com.petcare.app.data.remote.listenAsFlow
import com.petcare.app.model.AppointmentStatus
import com.petcare.app.model.ProgressUpdate
import com.petcare.app.model.TreatmentResult
import com.petcare.app.model.VetAppointment
import com.petcare.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AppointmentRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val reminderScheduler: ReminderScheduler,
) {
    private val appointments get() = firestore.collection(FirestoreCollections.APPOINTMENTS)

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("ยังไม่ได้เข้าสู่ระบบ")

    // เรียงฝั่ง client เพื่อไม่ต้องสร้าง composite index บน Firestore
    private fun List<VetAppointment>.sortedByDateTime() = sortedWith(compareBy({ it.date }, { it.time }))

    /** นัดหมายทั้งหมดของผู้ใช้ปัจจุบัน (realtime) เรียงตามวันเวลา */
    fun observeAppointments(): Flow<List<VetAppointment>> =
        appointments.whereEqualTo("ownerId", requireUid())
            .listenAsFlow<VetAppointment>()
            .map { it.sortedByDateTime() }

    /** นัดหมายของสัตว์ตัวเดียว (ต้องกรอง ownerId ด้วยเพื่อให้ผ่าน security rules) */
    fun observeByPet(petId: String): Flow<List<VetAppointment>> =
        appointments.whereEqualTo("ownerId", requireUid())
            .whereEqualTo("petId", petId)
            .listenAsFlow<VetAppointment>()
            .map { it.sortedByDateTime() }

    fun observeAppointment(id: String): Flow<VetAppointment?> =
        appointments.document(id).listenAsFlow()

    /** เพิ่มหรือแก้ไข แล้วตั้งการเตือนใหม่ตาม reminderDaysBefore */
    suspend fun save(appointment: VetAppointment, petName: String): String {
        val uid = requireUid()
        val ref = if (appointment.id.isBlank()) appointments.document() else appointments.document(appointment.id)
        val saved = appointment.copy(id = ref.id, ownerId = uid)
        ref.set(saved).await()
        reminderScheduler.schedule(saved, petName)
        return ref.id
    }

    suspend fun setDone(appointment: VetAppointment, done: Boolean, petName: String) {
        val status = if (done) AppointmentStatus.DONE else AppointmentStatus.PENDING
        appointments.document(appointment.id).update("status", status.name).await()
        reminderScheduler.schedule(appointment.copy(status = status.name), petName)
    }

    /**
     * บันทึกผลการรักษาและปิดนัด (status = DONE)
     * ถ้ามี [followUp] จะสร้างนัดติดตามผลพร้อมกันใน batch เดียว แล้วโยง id ไว้ในผลการรักษา
     */
    suspend fun recordResult(
        appointment: VetAppointment,
        result: TreatmentResult,
        followUp: VetAppointment?,
        petName: String,
    ) {
        val uid = requireUid()
        val apptRef = appointments.document(appointment.id)
        val savedFollowUp = followUp?.copy(id = appointments.document().id, ownerId = uid, followUpOf = appointment.id)
        val updated = appointment.copy(
            ownerId = uid,
            status = AppointmentStatus.DONE.name,
            result = if (savedFollowUp != null) result.copy(followUpAppointmentId = savedFollowUp.id) else result,
        )
        firestore.runBatch { batch ->
            batch.set(apptRef, updated)
            if (savedFollowUp != null) batch.set(appointments.document(savedFollowUp.id), savedFollowUp)
        }.await()
        reminderScheduler.cancel(appointment.id)
        savedFollowUp?.let { reminderScheduler.schedule(it, petName) }
    }

    /** เพิ่มบันทึกอาการในไทม์ไลน์ติดตามผล */
    suspend fun addProgress(appointment: VetAppointment, update: ProgressUpdate) {
        val withId = if (update.id.isBlank()) update.copy(id = UUID.randomUUID().toString()) else update
        appointments.document(appointment.id).set(appointment.withProgress(withId)).await()
    }

    suspend fun deleteProgress(appointment: VetAppointment, updateId: String) {
        appointments.document(appointment.id).set(appointment.withoutProgress(updateId)).await()
    }

    suspend fun delete(appointmentId: String) {
        appointments.document(appointmentId).delete().await()
        reminderScheduler.cancel(appointmentId)
    }
}
