package com.example.roomservice.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().getReference("admins")

    fun signUpAdmin(
        name: String,
        email: String,
        phone: String,
        pass: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: ""
                    val adminData = mapOf(
                        "id" to uid,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "role" to "ADMIN"
                    )
                    db.child(uid).setValue(adminData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it.message ?: "Database Error") }
                } else {
                    onFailure(task.exception?.message ?: "Authentication Failed")
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun syncUserDataByPhone(
        phone: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.orderByChild("phone").equalTo(phone).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val adminData = snapshot.children.first().value as? Map<String, Any>
                if (adminData != null) {
                    onSuccess(adminData + ("role" to "ADMIN"))
                    return@addOnSuccessListener
                }
            }
            
            FirebaseDatabase.getInstance().getReference("staff_lookup")
                .orderByChild("phone").equalTo(phone).get().addOnSuccessListener { staffSnapshot ->
                    if (staffSnapshot.exists()) {
                        val staffData = staffSnapshot.children.first().value as? Map<String, Any>
                        if (staffData != null) {
                            onSuccess(staffData)
                            return@addOnSuccessListener
                        }
                    }
                    onFailure("Phone number not registered.")
                }.addOnFailureListener {
                    onFailure("Error syncing data from server")
                }
        }.addOnFailureListener {
            onFailure("Network Error: Failed to sync profile")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun loginAdmin(
        email: String,
        pass: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: auth.currentUser?.uid ?: ""
                    if (uid.isBlank()) {
                        onFailure("Login successful but user ID is missing.")
                        return@addOnCompleteListener
                    }
                    
                    db.child(uid).get().addOnSuccessListener { snapshot ->
                        val data = snapshot.value as? Map<String, Any>
                        if (data != null) {
                            com.example.roomservice.data.HotelSession.setHotelId(uid)
                            onSuccess(data)
                        } else {
                            // FALLBACK: If user exists in Auth but not in DB (e.g. manual creation)
                            // Create a basic profile automatically
                            val fallbackData = mapOf(
                                "id" to uid,
                                "name" to "Admin",
                                "email" to email,
                                "role" to "ADMIN"
                            )
                            db.child(uid).setValue(fallbackData)
                                .addOnSuccessListener {
                                    com.example.roomservice.data.HotelSession.setHotelId(uid)
                                    onSuccess(fallbackData)
                                }
                                .addOnFailureListener { e ->
                                    onFailure("Failed to create profile: ${e.localizedMessage}")
                                }
                        }
                    }.addOnFailureListener { e ->
                        onFailure("Failed to restore profile data: ${e.localizedMessage}")
                    }
                } else {
                    onFailure(task.exception?.message ?: "Login Failed")
                }
            }
    }
}
