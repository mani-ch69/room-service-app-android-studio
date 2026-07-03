package com.example.roomservice.data.model

import java.util.UUID

enum class TableStatus {
    FREE,
    OCCUPIED,
    BILLING,
    CLEANING
}

data class RestaurantTable(
    val id: String = UUID.randomUUID().toString(),
    val tableNumber: String,
    val capacity: Int = 4,
    var status: TableStatus = TableStatus.FREE,
    var currentOrderId: String? = null
)

data class RestaurantOrder(
    val id: String = UUID.randomUUID().toString(),
    val tableNumber: String,
    val items: List<OrderItem> = emptyList(),
    var status: RestaurantOrderStatus = RestaurantOrderStatus.PLACED,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RestaurantOrderStatus {
    PLACED,
    PREPARING,
    SERVED,
    PAID
}