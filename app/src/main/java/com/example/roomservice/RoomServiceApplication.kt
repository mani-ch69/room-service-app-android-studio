package com.example.roomservice

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class RoomServiceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firebase Offline Persistence for better performance
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Already set or error
        }
    }
}
