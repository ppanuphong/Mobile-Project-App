package com.petcare.app.model

/**
 * หมวดหมู่สัตว์เลี้ยงจาก PetCare Catalog API (hosting/api/pet-catalog.json)
 * หมวดสุนัข/แมวไม่มี [breeds] เพราะดึงจาก API ภายนอกแทน (ดู BreedRepository)
 */
data class PetCategory(
    val id: String,
    val name: String,
    val emoji: String,
    val breeds: List<CatalogBreed> = emptyList(),
)

/** สายพันธุ์ในแคตตาล็อก: ชื่อไทย + ชื่ออังกฤษ */
data class CatalogBreed(val name: String, val nameEn: String = "")

/** แปลงเป็น [Breed] ที่ช่องเลือกสายพันธุ์ใช้ ("ชื่อไทย (English)") */
fun CatalogBreed.toBreed(): Breed =
    if (nameEn.isBlank()) Breed(en = name) else Breed(en = nameEn, th = name)
