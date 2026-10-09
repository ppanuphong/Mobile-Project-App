package com.petcare.app.model

import java.time.LocalDate

/** นัดหมาย + สัตว์เลี้ยงเจ้าของนัด + สถานะที่คำนวณแล้ว สำหรับแสดงในรายการ */
data class AppointmentItem(
    val appointment: VetAppointment,
    val pet: Pet?,
    val status: DisplayStatus,
)

/** น้ำหนักล่าสุดจากผลการรักษาที่บันทึกไว้ (null = ยังไม่เคยบันทึก) */
fun List<VetAppointment>.latestWeightKg(): Double? =
    filter { (it.result?.weightKg ?: 0.0) > 0 }
        .maxByOrNull { it.date }
        ?.result?.weightKg

fun List<VetAppointment>.withPets(
    pets: List<Pet>,
    today: LocalDate = LocalDate.now(),
): List<AppointmentItem> {
    val byId = pets.associateBy { it.id }
    return map { AppointmentItem(it, byId[it.petId], it.displayStatus(today)) }
}
