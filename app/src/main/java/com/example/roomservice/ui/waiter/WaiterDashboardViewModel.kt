package com.example.roomservice.ui.waiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomservice.data.CallRepository
import com.example.roomservice.data.ChatRepository
import com.example.roomservice.data.MenuRepository
import com.example.roomservice.data.OrderRepository
import com.example.roomservice.data.RoomRepository
import com.example.roomservice.data.HotelSession
import com.example.roomservice.data.StaffRepository
import com.example.roomservice.data.model.CallRequest
import com.example.roomservice.data.model.CallStatus
import com.example.roomservice.data.model.Order
import com.example.roomservice.data.model.OrderStatus
import com.example.roomservice.data.model.Staff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

data class WaiterDashboardUiState(
    val activeCalls: List<CallRequest> = emptyList(),
    val activeOrders: List<Order> = emptyList(),
    val roomStatuses: List<RoomLiveStatus> = emptyList(),
    val staffList: List<Staff> = emptyList()
)

class WaiterDashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WaiterDashboardUiState())
    val uiState: StateFlow<WaiterDashboardUiState> = _uiState.asStateFlow()

    init {
        // Combined stream for real-time entanglement
        viewModelScope.launch {
            combine(
                CallRepository.calls,
                OrderRepository.orders,
                RoomRepository.rooms,
                ChatRepository.messages,
                StaffRepository.staffList
            ) { calls, orders, rooms, messages, staff ->
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val recentOrders = orders.filter { it.timestamp > sevenDaysAgo }
                
                WaiterDashboardUiState(
                    activeCalls = calls.filter { it.status == CallStatus.PENDING }.sortedByDescending { it.timestamp },
                    activeOrders = recentOrders.filter { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED }.sortedByDescending { it.timestamp },
                    roomStatuses = rooms.map { room ->
                        RoomLiveStatus(
                            room = room,
                            activeCall = calls.find { it.roomNumber == room.roomNumber && it.status == CallStatus.PENDING },
                            activeOrder = recentOrders.find { it.roomNumber == room.roomNumber && it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED },
                            lastMessage = messages.filter { it.senderId == room.roomNumber }.lastOrNull()
                        )
                    },
                    staffList = staff
                )
            }
            .flowOn(Dispatchers.Default)
            .collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun markAsAttended(callId: String) {
        CallRepository.attendCall(callId)
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        OrderRepository.updateOrderStatus(orderId, newStatus)
    }

    fun refreshData() {
        HotelSession.hotelId.value?.let { HotelSession.setHotelId(it) }
    }

    fun assignStaffToOrder(orderId: String, staff: Staff) {
        OrderRepository.assignStaffToOrder(orderId, staff.id, staff.name)
    }

    fun assignStaffToCall(callId: String, staff: Staff) {
        CallRepository.assignStaffToCall(callId, staff.id, staff.name)
    }
}
