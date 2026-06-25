package com.example.roomservice.util

import android.content.Context
import android.content.SharedPreferences

class SecurityManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_security_prefs", Context.MODE_PRIVATE)

    fun setLoggedIn(isLoggedIn: Boolean, email: String? = null, name: String? = null, photo: String? = null, phone: String? = null, role: String = "ADMIN", hotelId: String? = null) {
        prefs.edit().apply {
            putBoolean("is_logged_in", isLoggedIn)
            putString("user_email", email)
            putString("user_name", name)
            putString("user_photo", photo)
            putString("user_phone", phone)
            putString("user_role", role)
            putString("hotel_id", hotelId)
            apply()
        }
    }

    fun getHotelId(): String? = prefs.getString("hotel_id", null)

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)
    fun getUserRole(): String = prefs.getString("user_role", "ADMIN") ?: "ADMIN"

    fun getUserData(): Map<String, String?> {
        return mapOf(
            "email" to prefs.getString("user_email", ""),
            "name" to prefs.getString("user_name", "Admin Profile"),
            "photo" to prefs.getString("user_photo", null),
            "phone" to prefs.getString("user_phone", "+91 ******0000"),
            "role" to prefs.getString("user_role", "ADMIN")
        )
    }

    fun setAppLock(pin: String, biometric: Boolean) {
        prefs.edit().apply {
            putString("app_pin", pin)
            putBoolean("use_biometric", biometric)
            putBoolean("is_lock_enabled", pin.isNotEmpty())
            putBoolean("has_seen_security_setup", true)
            apply()
        }
    }

    fun disableLock() {
        prefs.edit().apply {
            putBoolean("is_lock_enabled", false)
            putBoolean("has_seen_security_setup", true)
            apply()
        }
    }

    fun hasSeenSecuritySetup(): Boolean = prefs.getBoolean("has_seen_security_setup", false)

    fun isLockEnabled(): Boolean = prefs.getBoolean("is_lock_enabled", false)
    fun getPin(): String = prefs.getString("app_pin", "") ?: ""
    fun useBiometric(): Boolean = prefs.getBoolean("use_biometric", false)

    fun logout() {
        // 1. Clear SharedPreferences (Settings)
        prefs.edit().clear().apply()
        
        // 2. Clear Hotel Session and Repositories
        com.example.roomservice.data.HotelSession.clear()

        // 3. Clear App Cache
        try {
            val cacheDir = context.cacheDir
            cacheDir?.deleteRecursively()
            val codeCacheDir = context.codeCacheDir
            codeCacheDir?.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Sign out from Firebase
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
    }

    fun setRoomViewMode(mode: String) {
        prefs.edit().putString("room_view_mode", mode).apply()
    }

    fun getRoomViewMode(): String {
        return prefs.getString("room_view_mode", "list") ?: "list"
    }
}
