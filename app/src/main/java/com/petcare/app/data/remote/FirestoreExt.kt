package com.petcare.app.data.remote

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** ชื่อ collection ใน Cloud Firestore */
object FirestoreCollections {
    const val USERS = "users"
    const val PETS = "pets"
    const val APPOINTMENTS = "appointments"
}

/**
 * แปลง Query เป็น Flow ที่อัปเดตแบบ realtime ผ่าน addSnapshotListener
 * ทุกครั้งที่ข้อมูลบนเซิร์ฟเวอร์ (หรือ cache ในเครื่อง) เปลี่ยน Flow จะส่งรายการใหม่ออกมา
 * เมื่อ collector หยุดเก็บค่า listener จะถูกถอดออกอัตโนมัติ
 */
inline fun <reified T : Any> Query.listenAsFlow(): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w("Firestore", "listen failed", error)
            trySend(emptyList())
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot.toObjects(T::class.java))
    }
    awaitClose { registration.remove() }
}

/** เหมือน [listenAsFlow] แต่สำหรับเอกสารเดียว; ส่ง null เมื่อเอกสารถูกลบหรือไม่มีอยู่ */
inline fun <reified T : Any> DocumentReference.listenAsFlow(): Flow<T?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w("Firestore", "listen failed", error)
            trySend(null)
            return@addSnapshotListener
        }
        trySend(snapshot?.toObject(T::class.java))
    }
    awaitClose { registration.remove() }
}
