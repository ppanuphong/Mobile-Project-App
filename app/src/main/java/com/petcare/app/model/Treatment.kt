package com.petcare.app.model

import com.google.firebase.firestore.Exclude

/** ผลลัพธ์โดยรวมของการรักษาในนัดนั้น */
enum class TreatmentOutcome(val label: String) {
    NORMAL("ปกติ / เรียบร้อย"),
    ONGOING("อยู่ระหว่างรักษา"),
    RECOVERED("หายดีแล้ว");

    companion object {
        fun fromName(name: String) = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

/** อาการล่าสุดที่เจ้าของสังเกตได้ ใช้ในไทม์ไลน์ติดตามผล */
enum class PetCondition(val label: String) {
    BETTER("ดีขึ้น"),
    STABLE("ทรงตัว"),
    WORSE("แย่ลง"),
    RECOVERED("หายดี");

    companion object {
        fun fromName(name: String) = entries.firstOrNull { it.name == name } ?: STABLE
    }
}

/**
 * ผลการรักษาที่บันทึกหลังพบสัตวแพทย์ (ฝังอยู่ในเอกสารนัด `appointments/{id}.result`)
 * - weightKg / cost: 0 = ไม่ได้บันทึก
 * - followUpAppointmentId: นัดติดตามผลที่สร้างจากผลนี้ (ว่าง = ไม่มี)
 */
data class TreatmentResult(
    val diagnosis: String = "",
    val treatment: String = "",
    val medications: String = "",
    val weightKg: Double = 0.0,
    val cost: Double = 0.0,
    val outcome: String = TreatmentOutcome.NORMAL.name,
    val followUpAppointmentId: String = "",
    val recordedAt: String = "",
) {
    @get:Exclude
    val outcomeEnum: TreatmentOutcome
        get() = TreatmentOutcome.fromName(outcome)
}

/** บันทึกอาการ 1 ครั้ง (ฝังอยู่ใน `appointments/{id}.progress`) */
data class ProgressUpdate(
    val id: String = "",
    val date: String = "",
    val condition: String = PetCondition.STABLE.name,
    val note: String = "",
) {
    @get:Exclude
    val conditionEnum: PetCondition
        get() = PetCondition.fromName(condition)
}
