package com.example.roomservice.data

import com.example.roomservice.data.model.ChatMessage
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
import kotlinx.coroutines.withContext

object ChatRepository {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var db = FirebaseDatabase.getInstance().getReference("messages")
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    private var listener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("messages")
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    val list = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                    
                    // Notify for new messages from rooms
                    val currentMessages = _messages.value
                    list.forEach { msg ->
                        if (msg.senderId != "admin") {
                            if (currentMessages.none { it.id == msg.id }) {
                                NotificationRepository.addNotification(
                                    title = "New Message",
                                    message = "Room ${msg.roomNumber}: ${msg.text}",
                                    type = NotificationType.NEW_MESSAGE,
                                    roomNumber = msg.roomNumber
                                )
                            }
                        }
                    }

                    _messages.value = list
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

    fun clearData() {
        stopListening()
        _messages.value = emptyList()
    }

    fun sendMessage(roomNumber: String, text: String, senderId: String, imageUrl: String? = null, voiceUrl: String? = null) {
        val messageId = db.push().key ?: return
        val newMessage = ChatMessage(
            id = messageId,
            roomNumber = roomNumber,
            text = text,
            imageUrl = imageUrl,
            voiceUrl = voiceUrl,
            senderId = senderId,
            timestamp = System.currentTimeMillis()
        )
        
        db.child(messageId).setValue(newMessage)
    }
}
