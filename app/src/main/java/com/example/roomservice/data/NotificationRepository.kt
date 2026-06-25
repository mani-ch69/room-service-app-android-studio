package com.example.roomservice.data

import com.example.roomservice.data.model.Notification
import com.example.roomservice.data.model.NotificationType
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationRepository {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var db = FirebaseDatabase.getInstance().getReference("notifications")
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    private var listener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("notifications")
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    val list = snapshot.children.mapNotNull { it.getValue(Notification::class.java) }
                    _notifications.value = list.sortedByDescending { it.timestamp }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(listener!!)
    }

    private fun stopListening() {
        listener?.let { db.removeEventListener(it) }
        listener = null
    }

    fun addNotification(title: String, message: String, type: NotificationType, roomNumber: String) {
        val id = db.push().key ?: return
        val newNotification = Notification(
            id = id,
            title = title,
            message = message,
            type = type,
            roomNumber = roomNumber,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        db.child(id).setValue(newNotification)
    }

    fun markAsRead(id: String) {
        db.child(id).child("isRead").setValue(true)
    }

    fun markAllAsRead() {
        val updates = mutableMapOf<String, Any>()
        _notifications.value.forEach {
            if (!it.isRead) {
                updates["${it.id}/isRead"] = true
            }
        }
        if (updates.isNotEmpty()) {
            db.updateChildren(updates)
        }
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
