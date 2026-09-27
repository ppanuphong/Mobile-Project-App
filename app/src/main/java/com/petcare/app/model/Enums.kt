package com.petcare.app.model

enum class AppointmentKind(val label: String) {
    VACCINE("วัคซีน"),
    CHECKUP("ตรวจสุขภาพ"),
    TREATMENT("รักษา"),
    MEDICATION("ยา / ถ่ายพยาธิ"),
    GROOMING("อาบน้ำ ตัดขน"),
    OTHER("อื่น ๆ");

    companion object {
        fun fromName(name: String): AppointmentKind =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

enum class AppointmentStatus { PENDING, DONE }

enum class DisplayStatus(val label: String) {
    UPCOMING("กำลังจะถึง"),
    DUE_SOON("ใกล้ถึง"),
    OVERDUE("เลยกำหนด"),
    DONE("เสร็จแล้ว"),
}

/** ตัวกรองในหน้านัดหมาย */
enum class AppointmentFilter(val label: String) {
    ALL("ทั้งหมด"),
    UPCOMING("รอดำเนินการ"),
    OVERDUE("เลยกำหนด"),
    DONE("เสร็จแล้ว");

    fun matches(status: DisplayStatus): Boolean = when (this) {
        ALL -> true
        UPCOMING -> status == DisplayStatus.UPCOMING || status == DisplayStatus.DUE_SOON
        OVERDUE -> status == DisplayStatus.OVERDUE
        DONE -> status == DisplayStatus.DONE
    }
}

/** ชนิดสัตว์ที่มีให้เลือกในฟอร์ม (พิมพ์เองได้) */
val SPECIES_OPTIONS = listOf("สุนัข", "แมว", "กระต่าย", "นก", "ปลา", "หนูแฮมสเตอร์", "เต่า")

/** สีพื้นหลังรูปโปรไฟล์สัตว์เลี้ยง */
val AVATAR_COLORS = listOf(
    0xFF2E7D6FL, // teal
    0xFFE0A030L, // amber
    0xFFB5532EL, // rust
    0xFF5B7DB1L, // blue
    0xFF8E6BB0L, // purple
    0xFF6B8E4EL, // olive
    0xFFC0607AL, // rose
    0xFF6D6A63L, // stone
)
