package com.example.roomservice.data.model

enum class NotificationType {
    CALL_STAFF,
    NEW_ORDER,
    ORDER_ACCEPTED,
    ORDER_READY,
    ORDER_PICKED_UP,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    NEW_MESSAGE
}

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.NEW_ORDER,
    val roomNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
