package com.petcare.app.data

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.petcare.app.model.User
import com.petcare.app.notification.ReminderScheduler
import kotlinx.coroutines.tasks.await

/**
 * เข้าสู่ระบบ / สมัครสมาชิกด้วย Firebase Authentication (อีเมล + รหัสผ่าน)
 * ข้อมูลโปรไฟล์เก็บใน Firestore `users/{uid}` และบันทึก session ลง DataStore
 */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val reminderScheduler: ReminderScheduler,
    private val demoDataSeeder: DemoDataSeeder,
) {
    /** มี session ใน DataStore และ Firebase ยังจำผู้ใช้อยู่ → ไม่ต้อง login ซ้ำ */
    suspend fun hasValidSession(): Boolean {
        val session = sessionManager.currentSession() ?: return false
        val firebaseUser = auth.currentUser
        if (firebaseUser == null || firebaseUser.uid != session.userId) {
            sessionManager.clear()
            return false
        }
        return true
    }

    suspend fun login(email: String, password: String): Result<User> = runAuth {
        val firebaseUser = auth.signInWithEmailAndPassword(email, password).await().user
            ?: error("เข้าสู่ระบบไม่สำเร็จ")
        // บัญชีที่สร้างจาก Firebase Console อาจยังไม่มีโปรไฟล์ใน Firestore
        val user = userRepository.getUser(firebaseUser.uid)
            ?: User(id = firebaseUser.uid, name = email.substringBefore('@'), email = email)
                .also { userRepository.saveUser(it) }
        sessionManager.save(user)
        user
    }

    suspend fun register(name: String, phone: String, email: String, password: String): Result<User> =
        runAuth {
            val firebaseUser = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: error("สมัครสมาชิกไม่สำเร็จ")
            val user = User(id = firebaseUser.uid, name = name, phone = phone, email = email)
            userRepository.saveUser(user)
            sessionManager.save(user)
            user
        }

    /** เข้าบัญชีเดโม ถ้ายังไม่มีจะสร้างให้พร้อมข้อมูลตัวอย่าง */
    suspend fun loginDemo(): Result<User> {
        val result = login(DEMO_EMAIL, DEMO_PASSWORD)
        if (result.isSuccess) return result
        val created = register("ผู้ใช้ทดลอง", "081-234-5678", DEMO_EMAIL, DEMO_PASSWORD)
        created.getOrNull()?.let { runCatching { demoDataSeeder.seed() } }
        // ถ้าสร้างไม่ได้เพราะบัญชีมีอยู่แล้ว แปลว่ารหัสผ่านเดโมถูกเปลี่ยน → แสดง error ของการ login
        return if (created.isSuccess) created else result
    }

    suspend fun logout() {
        reminderScheduler.cancelAll()
        auth.signOut()
        sessionManager.clear()
    }

    private suspend fun runAuth(block: suspend () -> User): Result<User> =
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(Exception(thaiMessage(e), e))
        }

    private fun thaiMessage(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร"
        is FirebaseAuthUserCollisionException -> "อีเมลนี้ถูกใช้สมัครแล้ว"
        is FirebaseAuthInvalidUserException -> "ไม่พบบัญชีนี้ หรือบัญชีถูกปิดใช้งาน"
        is FirebaseAuthInvalidCredentialsException -> "อีเมลหรือรหัสผ่านไม่ถูกต้อง"
        is FirebaseNetworkException -> "เชื่อมต่ออินเทอร์เน็ตไม่ได้ ลองใหม่อีกครั้ง"
        is FirebaseTooManyRequestsException -> "ลองหลายครั้งเกินไป กรุณารอสักครู่"
        else -> {
            Log.w("AuthRepository", "auth failed", e)
            if (e.message.orEmpty().contains("API key", ignoreCase = true)) {
                "ยังไม่ได้ตั้งค่า Firebase: กรุณาใส่ไฟล์ google-services.json ของโปรเจกต์คุณ"
            } else {
                "เกิดข้อผิดพลาด ลองใหม่อีกครั้ง"
            }
        }
    }

    companion object {
        const val DEMO_EMAIL = "demo@petcare.app"
        const val DEMO_PASSWORD = "demo1234"
    }
}
