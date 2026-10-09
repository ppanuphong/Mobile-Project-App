package com.petcare.app.model

/** แหล่งข้อมูลสายพันธุ์ของแต่ละหมวด */
enum class BreedSource(val label: String) {
    DOG_CEO("Dog CEO API"),
    CATFACT("catfact.ninja API"),
    CATALOG("PetCare Catalog API"),
}

/** หมวดหมู่สัตว์เลี้ยง (สุนัข แมว นก ฯลฯ) จาก Catalog API */
data class PetCategory(
    val id: String,
    val name: String,
    val emoji: String,
    val source: BreedSource,
    /** มีเฉพาะหมวดที่ข้อมูลอยู่ใน Catalog API เอง; สุนัข/แมวโหลดจาก API ภายนอก */
    val breeds: List<Breed> = emptyList(),
)

/**
 * สายพันธุ์ 1 รายการ
 * @param species ชนิดสัตว์ที่จะบันทึกลง Pet.species (null = ใช้ชื่อหมวด)
 * @param dogCeoPath path ของ Dog CEO API เช่น "bulldog/french" ใช้ดึงรูป
 */
data class Breed(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val species: String? = null,
    val dogCeoPath: String? = null,
) {
    companion object {
        /** ตัวเลือก "ไม่ทราบสายพันธุ์" — บันทึก breed เป็นค่าว่าง */
        val UNKNOWN = Breed(id = "unknown", name = "ไม่ทราบ / พันธุ์ผสม")
    }
}
