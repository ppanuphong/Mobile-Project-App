package com.petcare.app.model

import com.google.firebase.firestore.DocumentId

/**
 * ผู้ใช้ 1 คน เก็บที่ `users/{uid}` โดย uid มาจาก Firebase Authentication
 * รหัสผ่านไม่ได้เก็บที่นี่ — Firebase Auth ดูแลให้
 */
data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
)
