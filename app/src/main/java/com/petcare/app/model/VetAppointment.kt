package com.petcare.app.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.petcare.app.util.DateUtils
import java.time.LocalDate

/**
 * นัดหมอ / วัคซีน เก็บที่ `appointments/{id}`
 * - ownerId: เก็บซ้ำจาก Pet เพื่อให้ query ตามเจ้าของและเขียน security rules ได้ง่าย
 * - kind: ชื่อ [AppointmentKind]
 * - date: "yyyy-MM-dd", time: "HH:mm"
 * - status: ชื่อ [AppointmentStatus] ("PENDING" / "DONE")
 * - followUpOf: id ของนัดเดิม ถ้านัดนี้เป็นนัดติดตามผล
 * - result: ผลการรักษาที่บันทึกหลังพบหมอ (null = ยังไม่บันทึก)
 * - progress: ไทม์ไลน์ติดตามอาการหลังการรักษา เรียงตามวันที่บันทึก
 */
data class VetAppointment(
    @DocumentId val id: String = "",
    val petId: String = "",
    val ownerId: String = "",
    val kind: String = AppointmentKind.CHECKUP.name,
    val title: String = "",
    val date: String = "",
    val time: String = "",
    val vetName: String = "",
    val clinic: String = "",
    val notes: String = "",
    val reminderDaysBefore: Int = 1,
    val status: String = AppointmentStatus.PENDING.name,
    val followUpOf: String = "",
    val result: TreatmentResult? = null,
    val progress: List<ProgressUpdate> = emptyList(),
) {
    @get:Exclude
    val kindEnum: AppointmentKind
        get() = AppointmentKind.fromName(kind)

    @get:Exclude
    val isDone: Boolean
        get() = status == AppointmentStatus.DONE.name

    /** เคสที่ยังต้องติดตามอาการ */
    @get:Exclude
    val isUnderTreatment: Boolean
        get() = result?.outcomeEnum == TreatmentOutcome.ONGOING

    /** อาการล่าสุดที่บันทึกไว้ (ล่าสุดตามวันที่) */
    @get:Exclude
    val latestProgress: ProgressUpdate?
        get() = progress.maxWithOrNull(compareBy({ it.date }, { it.id }))

    /**
     * เพิ่มบันทึกอาการ ถ้าเลือก "หายดี" จะปิดเคส (outcome = RECOVERED)
     * ถ้าเคยปิดเคสแล้วแต่อาการกลับมาแย่ลง จะเปิดเคสใหม่เป็น ONGOING
     */
    fun withProgress(update: ProgressUpdate): VetAppointment {
        val current = result ?: TreatmentResult()
        val outcome = when (update.conditionEnum) {
            PetCondition.RECOVERED -> TreatmentOutcome.RECOVERED
            PetCondition.WORSE -> TreatmentOutcome.ONGOING
            else -> current.outcomeEnum
        }
        return copy(progress = progress + update, result = current.copy(outcome = outcome.name))
    }

    fun withoutProgress(updateId: String): VetAppointment = copy(progress = progress.filterNot { it.id == updateId })

    /** สถานะสำหรับแสดงผล — "เลยกำหนด" คำนวณจากวันที่ ไม่ได้เก็บลงฐานข้อมูล */
    fun displayStatus(today: LocalDate = LocalDate.now()): DisplayStatus {
        if (isDone) return DisplayStatus.DONE
        val days = DateUtils.daysUntil(date, today) ?: return DisplayStatus.UPCOMING
        return when {
            days < 0 -> DisplayStatus.OVERDUE
            days <= maxOf(reminderDaysBefore, DUE_SOON_DAYS) -> DisplayStatus.DUE_SOON
            else -> DisplayStatus.UPCOMING
        }
    }

    /** ถึงช่วงที่ต้องเตือนแล้วหรือยัง (วันนี้ >= วันนัด - reminderDaysBefore) และยังไม่เสร็จ */
    fun isInReminderWindow(today: LocalDate = LocalDate.now()): Boolean {
        if (isDone) return false
        val days = DateUtils.daysUntil(date, today) ?: return false
        return days <= reminderDaysBefore
    }

    companion object {
        const val DUE_SOON_DAYS = 7
    }
}
