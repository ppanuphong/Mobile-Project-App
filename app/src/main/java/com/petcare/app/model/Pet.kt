package com.petcare.app.model

import com.google.firebase.firestore.DocumentId

/**
 * สัตว์เลี้ยง เก็บที่ `pets/{petId}`
 * - birthday: "yyyy-MM-dd" (ว่างได้ถ้าไม่ทราบ)
 * - avatarColor: สี ARGB แบบ Long ใช้เป็นพื้นหลังรูปโปรไฟล์
 */
data class Pet(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val birthday: String = "",
    val avatarColor: Long = 0xFF2E7D6FL,
)
