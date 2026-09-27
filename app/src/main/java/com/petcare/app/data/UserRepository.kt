package com.petcare.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.petcare.app.data.remote.FirestoreCollections
import com.petcare.app.model.User
import kotlinx.coroutines.tasks.await

class UserRepository(private val firestore: FirebaseFirestore) {

    private fun doc(uid: String) = firestore.collection(FirestoreCollections.USERS).document(uid)

    suspend fun getUser(uid: String): User? =
        doc(uid).get().await().toObject(User::class.java)

    suspend fun saveUser(user: User) {
        doc(user.id).set(user).await()
    }
}
