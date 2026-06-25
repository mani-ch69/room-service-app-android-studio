package com.example.roomservice.data

import com.example.roomservice.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RestaurantRepository {
    private val _tables = MutableStateFlow<List<RestaurantTable>>(
        (1..12).map { RestaurantTable(tableNumber = "T-$it") }
    )
    val tables: StateFlow<List<RestaurantTable>> = _tables.asStateFlow()

    private val _activeOrders = MutableStateFlow<List<RestaurantOrder>>(emptyList())
    val activeOrders: StateFlow<List<RestaurantOrder>> = _activeOrders.asStateFlow()

    fun updateTableStatus(tableNumber: String, newStatus: TableStatus) {
        _tables.value = _tables.value.map {
            if (it.tableNumber == tableNumber) it.copy(status = newStatus) else it
        }
    }

    fun placeOrder(order: RestaurantOrder) {
        _activeOrders.value = _activeOrders.value + order
        updateTableStatus(order.tableNumber, TableStatus.OCCUPIED)
    }

    fun updateOrderStatus(orderId: String, newStatus: RestaurantOrderStatus) {
        _activeOrders.value = _activeOrders.value.map {
            if (it.id == orderId) it.copy(status = newStatus) else it
        }
        
        if (newStatus == RestaurantOrderStatus.PAID) {
            val order = _activeOrders.value.find { it.id == orderId }
            order?.let { updateTableStatus(it.tableNumber, TableStatus.CLEANING) }
            _activeOrders.value = _activeOrders.value.filter { it.id != orderId }
        }
    }
}