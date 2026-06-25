package com.example.roomservice.data.model

import java.util.UUID

data class ChatMessage(
    var id: String = UUID.randomUUID().toString(),
    var roomNumber: String = "",
    var text: String = "",
    var imageUrl: String? = null,
    var voiceUrl: String? = null,
    var senderId: String = "",
    var timestamp: Long = System.currentTimeMillis()
)