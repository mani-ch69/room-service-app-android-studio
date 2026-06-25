package com.example.roomservice.data

import com.example.roomservice.data.model.NotificationType
import com.example.roomservice.data.model.Order
import com.example.roomservice.data.model.OrderStatus
import com.example.roomservice.data.model.StatusUpdate
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OrderRepository {
    private var db = FirebaseDatabase.getInstance().getReference("orders")
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    private var listener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("orders")
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Order::class.java) }
                
                // Track processed orders in-memory
                val pendingOrders = list.filter { it.status == OrderStatus.PENDING }
                
                // On first load, just fill the set without triggering notifications
                if (_isFirstLoad) {
                    pendingOrders.forEach { _processedOrderIds.add(it.id) }
                    _isFirstLoad = false
                } else {
                    pendingOrders.forEach { order ->
                        if (!_processedOrderIds.contains(order.id)) {
                            _processedOrderIds.add(order.id)
                            
                            // Only notify for truly new orders (placed within last minute to be safe)
                            if (System.currentTimeMillis() - order.timestamp < 60000) {
                                val itemsList = order.items.joinToString { "${it.quantity}x ${it.name}" }
                                val msg = "📦 New Order Placed: #${order.id.takeLast(6)}\nItems: $itemsList\nTotal: ₹${order.totalAmount}"
                                ChatRepository.sendMessage(order.roomNumber, msg, order.roomNumber)
                                
                                NotificationRepository.addNotification(
                                    title = "New Order",
                                    message = "Room ${order.roomNumber} placed a new order: #${order.id.takeLast(6)}",
                                    type = NotificationType.NEW_ORDER,
                                    roomNumber = order.roomNumber
                                )
                            }
                        }
                    }
                }

                _orders.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(listener!!)
        db.keepSynced(true)
    }

    private val _processedOrderIds = mutableSetOf<String>()
    private var _isFirstLoad = true

    private fun stopListening() {
        listener?.let { db.removeEventListener(it) }
        listener = null
        _isFirstLoad = true
    }

    fun clearData() {
        stopListening()
        _orders.value = emptyList()
        _processedOrderIds.clear()
    }

    fun placeOrder(order: Order) {
        val orderWithHistory = order.copy(
            statusHistory = order.statusHistory + StatusUpdate(order.status, System.currentTimeMillis())
        )
        db.child(orderWithHistory.id).setValue(orderWithHistory)
        
        // Also send a message to the chat
        val itemsList = orderWithHistory.items.joinToString { "${it.quantity}x ${it.name}" }
        val orderMessage = "📦 New Order Placed\nItems: $itemsList\nTotal: ₹${orderWithHistory.totalAmount}\nPayment: ${orderWithHistory.paymentMethod}"
        
        ChatRepository.sendMessage(
            roomNumber = orderWithHistory.roomNumber,
            text = orderMessage,
            senderId = orderWithHistory.roomNumber // Mark as room sender so it shows up correctly in chat
        )
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        db.child(orderId).child("status").setValue(newStatus)
        
        // Notify in chat and notification bell about status update
        val order = _orders.value.find { it.id == orderId }
        if (order != null) {
            val (statusText, notifType) = when(newStatus) {
                OrderStatus.ACCEPTED -> "✅ Order Accepted" to NotificationType.ORDER_ACCEPTED
                OrderStatus.PROCESSING -> "👨‍🍳 Preparing your food" to NotificationType.ORDER_READY // Using ready as prep
                OrderStatus.READY -> "🛎️ Order is Ready" to NotificationType.ORDER_READY
                OrderStatus.PICKED_UP -> "🚚 Out for delivery" to NotificationType.ORDER_PICKED_UP
                OrderStatus.ARRIVED -> "📍 Staff has arrived at your door" to NotificationType.ORDER_PICKED_UP
                OrderStatus.DELIVERED -> "🏁 Order Delivered" to NotificationType.ORDER_DELIVERED
                OrderStatus.CANCELLED -> "❌ Order Cancelled" to NotificationType.ORDER_CANCELLED
                else -> "🔄 Status: ${newStatus.name}" to NotificationType.NEW_ORDER
            }

            ChatRepository.sendMessage(
                roomNumber = order.roomNumber,
                text = "Order Update: $statusText",
                senderId = "admin"
            )

            NotificationRepository.addNotification(
                title = "Order Updated",
                message = "Room ${order.roomNumber} order #${order.id.takeLast(6)}: $statusText",
                type = notifType,
                roomNumber = order.roomNumber
            )
        }
    }

    fun acceptOrderAndStartProcessing(orderId: String) {
        // Skips the 'Accepted' manual step and goes straight to 'Processing'
        db.child(orderId).child("status").setValue(OrderStatus.PROCESSING)
    }

    fun markOrderAsReadyAndAssignWaiter(orderId: String, availableWaiters: List<com.example.roomservice.data.model.Staff>) {
        val pin = (1000..9999).random().toString()
        db.child(orderId).child("deliveryPin").setValue(pin)
        db.child(orderId).child("status").setValue(OrderStatus.READY)

        // Automatic Assignment: Pick the first available waiter
        val freeWaiter = availableWaiters.find { it.role == "Waiter" && it.isAvailable }
        if (freeWaiter != null) {
            assignStaffToOrder(orderId, freeWaiter.id, freeWaiter.name)
            // Note: assignStaffToOrder internally sets status to PROCESSING, 
            // so we must ensure it stays READY for pickup.
            db.child(orderId).child("status").setValue(OrderStatus.READY)
        }
    }

    fun assignStaffToOrder(orderId: String, staffId: String, staffName: String) {
        db.child(orderId).child("assignedStaffId").setValue(staffId)
        db.child(orderId).child("assignedStaffName").setValue(staffName)
        db.child(orderId).child("status").setValue(OrderStatus.PROCESSING)
    }

    fun unassignStaffFromAllTasks(staffId: String) {
        _orders.value.forEach { order ->
            if (order.assignedStaffId == staffId && order.status != OrderStatus.DELIVERED) {
                db.child(order.id).child("assignedStaffId").removeValue()
                db.child(order.id).child("assignedStaffName").removeValue()
            }
        }
    }

    fun verifyDeliveryPin(orderId: String, pin: String): Boolean {
        val order = _orders.value.find { it.id == orderId }
        if (order != null && order.deliveryPin == pin) {
            updateOrderStatus(orderId, OrderStatus.DELIVERED)
            return true
        }
        return false
    }
}
