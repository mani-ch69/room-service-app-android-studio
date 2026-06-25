package com.example.roomservice.data.model

import java.util.UUID

data class Order(
    var id: String = UUID.randomUUID().toString(),
    var roomNumber: String = "",
    var items: List<OrderItem> = emptyList(),
    var subtotal: Double = 0.0,
    var tax: Double = 0.0,
    var totalAmount: Double = 0.0,
    var paymentMethod: String = "Cash on Delivery",
    var status: OrderStatus = OrderStatus.PENDING,
    var timestamp: Long = System.currentTimeMillis(),
    var notes: String = "",
    var deliveryPin: String = "",
    var rewardEarned: Int = 0,
    var statusHistory: List<StatusUpdate> = emptyList(),
    var assignedStaffId: String? = null,
    var assignedStaffName: String? = null
)

data class StatusUpdate(
    var status: OrderStatus = OrderStatus.PENDING,
    var timestamp: Long = System.currentTimeMillis()
)

data class OrderItem(
    var menuItemId: String = "",
    var name: String = "",
    var price: Double = 0.0,
    var quantity: Int = 0
)

enum class OrderStatus {
    CART_DRAFT,
    PENDING,
    ACCEPTED,      // Restaurant Accepted
    PROCESSING,    // Preparing
    READY,         // Ready for Pickup (PIN Generated)
    PICKED_UP,     // Out for Delivery
    ARRIVED,       // At Guest Door
    DELIVERED,     // Completed
    CANCELLED,
    NOT_ACCEPTED
}