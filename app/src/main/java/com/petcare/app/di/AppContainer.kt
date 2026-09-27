package com.petcare.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.petcare.app.BuildConfig
import com.petcare.app.data.AppointmentRepository
import com.petcare.app.data.AuthRepository
import com.petcare.app.data.DemoDataSeeder
import com.petcare.app.data.PetRepository
import com.petcare.app.data.SessionManager
import com.petcare.app.data.UserRepository
import com.petcare.app.notification.ReminderScheduler

/** Dependency injection แบบ manual: สร้าง repository ทุกตัวครั้งเดียวต่อแอป */
class AppContainer(context: Context) {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance().apply {
            if (BuildConfig.USE_FIREBASE_EMULATOR) useEmulator(EMULATOR_HOST, 9099)
        }
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            // เก็บ cache ในเครื่องเพื่อให้เปิดดูข้อมูลได้แม้ออฟไลน์
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            }
            if (BuildConfig.USE_FIREBASE_EMULATOR) useEmulator(EMULATOR_HOST, 8080)
        }
    }

    val sessionManager = SessionManager(context)
    val reminderScheduler = ReminderScheduler(context)

    val userRepository by lazy { UserRepository(firestore) }
    val petRepository by lazy { PetRepository(firestore, auth, reminderScheduler) }
    val appointmentRepository by lazy { AppointmentRepository(firestore, auth, reminderScheduler) }
    val authRepository by lazy {
        AuthRepository(
            auth = auth,
            userRepository = userRepository,
            sessionManager = sessionManager,
            reminderScheduler = reminderScheduler,
            demoDataSeeder = DemoDataSeeder(petRepository, appointmentRepository),
        )
    }

    private companion object {
        /**
         * ใช้ร่วมกับ `adb reverse tcp:9099 tcp:9099` และ `adb reverse tcp:8080 tcp:8080`
         * (ไม่ใช้ 10.0.2.2 เพราะ Android 17 จำกัดการเข้าถึงเครือข่ายภายในของแอป)
         */
        const val EMULATOR_HOST = "127.0.0.1"
    }
}
