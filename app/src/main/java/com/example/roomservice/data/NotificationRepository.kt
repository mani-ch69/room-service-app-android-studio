package com.example.roomservice.data

import com.example.roomservice.data.model.Notification
import com.example.roomservice.data.model.NotificationType
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

object NotificationRepository {
    private var db = FirebaseDatabase.getInstance().getReference("notifications")
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications
    private var listener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("notifications")
        // Optimization: Fetch only unread or recent notifications
        val query = db.orderByChild("timestamp").limitToLast(50)
        
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Notification::class.java) }
                _notifications.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        query.addValueEventListener(listener!!)
    }

    private fun stopListening() {
        listener?.let { db.removeEventListener(it) }
        listener = null
    }

    fun addNotification(hotelId: String, title: String, type: NotificationType, message: String, roomNumber: String = "") {
        val id = UUID.randomUUID().toString()
        val notif = Notification(id, title, message, type, roomNumber, System.currentTimeMillis())
        FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("notifications").child(id).setValue(notif)
    }

    fun markAsRead(id: String) {
        db.child(id).child("read").setValue(true)
    }

    fun markAllAsRead() {
        _notifications.value.filter { !it.isRead }.forEach { markAsRead(it.id) }
    }

    fun deleteNotification(id: String) {
        db.child(id).removeValue()
    }

    fun clearNotifications() {
        db.removeValue()
    }

    fun clearData() {
        stopListening()
        _notifications.value = emptyList()
    }
}
