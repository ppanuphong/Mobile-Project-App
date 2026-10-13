package com.petcare.app.model

import com.google.firebase.firestore.DocumentId

/**
 * ผู้ใช้ 1 คน เก็บที่ `users/{uid}` โดย uid มาจาก Firebase Authentication
 * รหัสผ่านไม่ได้เก็บที่นี่ — Firebase Auth ดูแลให้
 * - photo / coverPhoto: รูปโปรไฟล์ (480x480) และรูปหน้าปก (960x360) แบบ JPEG Base64, ว่าง = ไม่มีรูป
 * - bio: คำแนะนำตัวสั้น ๆ, location: อาศัยอยู่ที่
 */
data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val photo: String = "",
    val coverPhoto: String = "",
    val bio: String = "",
    val location: String = "",
)
