package com.example.roomservice.ui.restaurant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomservice.data.RestaurantRepository
import com.example.roomservice.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RestaurantUiState(
    val tables: List<RestaurantTable> = emptyList(),
    val activeOrders: List<RestaurantOrder> = emptyList(),
    val freeTablesCount: Int = 0,
    val busyTablesCount: Int = 0,
    val pendingOrdersCount: Int = 0
)

class RestaurantViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RestaurantUiState())
    val uiState: StateFlow<RestaurantUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                RestaurantRepository.tables,
                RestaurantRepository.activeOrders
            ) { tables, orders ->
                RestaurantUiState(
                    tables = tables,
                    activeOrders = orders,
                    freeTablesCount = tables.count { it.status == TableStatus.FREE || it.status == TableStatus.CLEANING },
                    busyTablesCount = tables.count { it.status == TableStatus.OCCUPIED || it.status == TableStatus.BILLING },
                    pendingOrdersCount = orders.count { it.status == RestaurantOrderStatus.PLACED || it.status == RestaurantOrderStatus.PREPARING }
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun onTableClick(table: RestaurantTable) {
        // Logic for table click (e.g., change status or open order)
        val nextStatus = when (table.status) {
            TableStatus.FREE -> TableStatus.OCCUPIED
            TableStatus.OCCUPIED -> TableStatus.BILLING
            TableStatus.BILLING -> TableStatus.CLEANING
            TableStatus.CLEANING -> TableStatus.FREE
        }
        RestaurantRepository.updateTableStatus(table.tableNumber, nextStatus)
    }

    fun updateOrderStatus(orderId: String, newStatus: RestaurantOrderStatus) {
        RestaurantRepository.updateOrderStatus(orderId, newStatus)
    }
}