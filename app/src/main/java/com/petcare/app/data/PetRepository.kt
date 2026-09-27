package com.petcare.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.petcare.app.data.remote.FirestoreCollections
import com.petcare.app.data.remote.listenAsFlow
import com.petcare.app.model.Pet
import com.petcare.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PetRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val reminderScheduler: ReminderScheduler,
) {
    private val pets get() = firestore.collection(FirestoreCollections.PETS)
    private val appointments get() = firestore.collection(FirestoreCollections.APPOINTMENTS)

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("ยังไม่ได้เข้าสู่ระบบ")

    /** สัตว์เลี้ยงทั้งหมดของผู้ใช้ปัจจุบัน (realtime) เรียงตามชื่อ */
    fun observePets(): Flow<List<Pet>> =
        pets.whereEqualTo("ownerId", requireUid())
            .listenAsFlow<Pet>()
            .map { list -> list.sortedBy { it.name.lowercase() } }

    fun observePet(petId: String): Flow<Pet?> = pets.document(petId).listenAsFlow()

    /** เพิ่มใหม่ถ้า id ว่าง ไม่งั้นเขียนทับ คืนค่า id ของเอกสาร */
    suspend fun savePet(pet: Pet): String {
        val uid = requireUid()
        val ref = if (pet.id.isBlank()) pets.document() else pets.document(pet.id)
        ref.set(pet.copy(ownerId = uid)).await()
        return ref.id
    }

    /** ลบสัตว์เลี้ยงพร้อมนัดหมายทั้งหมดของตัวนั้นใน batch เดียว และยกเลิกการเตือน */
    suspend fun deletePet(petId: String) {
        val uid = requireUid()
        val related = appointments
            .whereEqualTo("ownerId", uid)
            .whereEqualTo("petId", petId)
            .get().await()

        firestore.runBatch { batch ->
            related.documents.forEach { batch.delete(it.reference) }
            batch.delete(pets.document(petId))
        }.await()

        related.documents.forEach { reminderScheduler.cancel(it.id) }
    }
}
