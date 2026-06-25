package com.example.roomservice.ui.waiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomservice.data.CallRepository
import com.example.roomservice.data.ChatRepository
import com.example.roomservice.data.MenuRepository
import com.example.roomservice.data.RoomRepository
import com.example.roomservice.data.NotificationRepository
import com.example.roomservice.data.OrderRepository
import com.example.roomservice.data.StaffRepository
import com.example.roomservice.data.model.CallRequest
import com.example.roomservice.data.model.CallStatus
import com.example.roomservice.data.model.Category
import com.example.roomservice.data.model.ChatMessage
import com.example.roomservice.data.model.MenuItem
import com.example.roomservice.data.model.Room
import com.example.roomservice.data.model.Order
import com.example.roomservice.data.model.OrderStatus
import com.example.roomservice.data.model.Notification
import com.example.roomservice.data.model.Staff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

data class RoomLiveStatus(
    val room: Room,
    val activeCall: CallRequest? = null,
    val activeOrder: Order? = null,
    val lastMessage: ChatMessage? = null
)

data class DashboardStats(
    val activeCalls: Int = 0,
    val newOrders: Int = 0,
    val processingOrders: Int = 0,
    val deliveredOrders: Int = 0,
    val cancelledOrders: Int = 0
)

class AdminMenuViewModel : ViewModel() {
    val menuItems: StateFlow<List<MenuItem>> = MenuRepository.menuItems
    val categories: StateFlow<List<Category>> = MenuRepository.categories
    val shopItems: StateFlow<List<MenuItem>> = com.example.roomservice.data.ShopRepository.shopItems
    val shopCategories: StateFlow<List<Category>> = com.example.roomservice.data.ShopRepository.categories
    val notifications: StateFlow<List<Notification>> = NotificationRepository.notifications
    val staffList: StateFlow<List<Staff>> = StaffRepository.staffList

    private val _latestIncomingOrder = MutableStateFlow<Order?>(null)
    val latestIncomingOrder = _latestIncomingOrder.asStateFlow()
    
    private val _notificationSignal = MutableSharedFlow<String>(replay = 0)
    val notificationSignal = _notificationSignal.asSharedFlow()
    
    val unreadNotificationsCount: StateFlow<Int> = NotificationRepository.notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dashboardStats: StateFlow<DashboardStats> = OrderRepository.orders
        .combine(CallRepository.calls) { orders, calls ->
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val recentOrders = orders.filter { it.timestamp > sevenDaysAgo }
            
            DashboardStats(
                activeCalls = calls.count { it.status == CallStatus.PENDING },
                newOrders = recentOrders.count { it.status == OrderStatus.PENDING },
                processingOrders = recentOrders.count { it.status == OrderStatus.PROCESSING || it.status == OrderStatus.ACCEPTED || it.status == OrderStatus.ARRIVED },
                deliveredOrders = recentOrders.count { it.status == OrderStatus.DELIVERED },
                cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED || it.status == OrderStatus.NOT_ACCEPTED || it.timestamp <= sevenDaysAgo }
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val allOrders: StateFlow<List<Order>> = OrderRepository.orders
        .map { orders ->
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            orders.filter { it.timestamp > sevenDaysAgo }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedOrders: StateFlow<List<Order>> = OrderRepository.orders
        .map { orders ->
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            orders.filter { 
                it.status == OrderStatus.CANCELLED || 
                it.status == OrderStatus.NOT_ACCEPTED || 
                it.timestamp <= sevenDaysAgo 
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCalls: StateFlow<List<com.example.roomservice.data.model.CallRequest>> = com.example.roomservice.data.CallRepository.calls
    val bookings: StateFlow<List<com.example.roomservice.data.model.Booking>> = com.example.roomservice.data.BookingRepository.bookings

    val roomLiveStatuses: StateFlow<List<RoomLiveStatus>> = combine(
        RoomRepository.rooms,
        CallRepository.calls,
        ChatRepository.messages,
        OrderRepository.orders
    ) { rooms, calls, messages, orders ->
        rooms.map { room ->
            val normalizedRoom = room.roomNumber.trim().lowercase()
            
            RoomLiveStatus(
                room = room,
                activeCall = calls.find { 
                    it.roomNumber.trim().lowercase() == normalizedRoom && 
                    it.status == CallStatus.PENDING 
                },
                activeOrder = orders.find { 
                    it.roomNumber.trim().lowercase() == normalizedRoom && 
                    it.status == OrderStatus.PENDING 
                },
                lastMessage = messages.filter { 
                    it.senderId.trim().lowercase() == normalizedRoom 
                }.lastOrNull()
            )
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            OrderRepository.orders.collect { orders ->
                val lastPending = orders.filter { it.status == OrderStatus.PENDING }.lastOrNull()
                if (lastPending != null && lastPending.id != _latestIncomingOrder.value?.id) {
                    _latestIncomingOrder.value = lastPending
                    _notificationSignal.emit("New Order: Room ${lastPending.roomNumber}")
                }
            }
        }
        
        viewModelScope.launch {
            CallRepository.calls.collect { calls ->
                val lastPendingCall = calls.filter { it.status == CallStatus.PENDING }.lastOrNull()
                // Simple logic: if count increases or id is new
                // For simplicity, just check if any pending call exists that we haven't handled
                // But let's just trigger sound on any new pending call
            }
        }
        
        // Better way to handle both:
        viewModelScope.launch {
            combine(OrderRepository.orders, CallRepository.calls) { orders, calls ->
                orders.count { it.status == OrderStatus.PENDING } + 
                calls.count { it.status == CallStatus.PENDING }
            }.collect { totalPending ->
                staticLastTotalPending = if (totalPending > staticLastTotalPending) {
                    _notificationSignal.emit("Update")
                    totalPending
                } else {
                    totalPending
                }
            }
        }
    }

    private companion object {
        var staticLastTotalPending = 0
    }

    fun dismissIncomingOrderPopup() {
        _latestIncomingOrder.value = null
    }

    fun acceptOrder(orderId: String) {
        OrderRepository.updateOrderStatus(orderId, OrderStatus.ACCEPTED)
        if (_latestIncomingOrder.value?.id == orderId) _latestIncomingOrder.value = null
    }

    fun cancelOrder(orderId: String) {
        OrderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED)
        if (_latestIncomingOrder.value?.id == orderId) _latestIncomingOrder.value = null
    }

    fun completeOrder(orderId: String) {
        OrderRepository.updateOrderStatus(orderId, OrderStatus.DELIVERED)
    }

    fun assignStaffToOrder(orderId: String, staff: Staff) {
        OrderRepository.assignStaffToOrder(orderId, staff.id, staff.name)
    }

    fun assignStaffToCall(callId: String, staff: Staff) {
        CallRepository.assignStaffToCall(callId, staff.id, staff.name)
    }

    fun toggleAvailability(itemId: String, currentStatus: Boolean) {
        MenuRepository.updateItemAvailability(itemId, !currentStatus)
    }

    fun removeItem(itemId: String) {
        MenuRepository.deleteItem(itemId)
    }

    fun addNewCategory(name: String) {
        if (name.isNotBlank()) MenuRepository.addCategory(name)
    }

    fun editCategory(categoryId: String, newName: String) {
        if (newName.isNotBlank()) MenuRepository.updateCategory(categoryId, newName)
    }

    fun removeCategory(categoryId: String) {
        MenuRepository.deleteCategory(categoryId)
    }

    fun removeStaff(staffId: String) {
        // Unassign all tasks first to prevent orphaned orders
        com.example.roomservice.data.OrderRepository.unassignStaffFromAllTasks(staffId)
        com.example.roomservice.data.CallRepository.unassignStaffFromAllCalls(staffId)

        StaffRepository.deleteStaff(staffId)
    }

    fun saveStaff(id: String?, name: String, phone: String, role: String) {
        val staffId = id ?: java.util.UUID.randomUUID().toString()
        val hId = com.example.roomservice.data.HotelSession.hotelId.value ?: ""
        
        val existingStaff = staffList.value.find { it.id == staffId }
        val code = existingStaff?.code ?: (1000..9999).random().toString()
        
        val staff = Staff(id = staffId, name = name, phone = phone, role = role, code = code, hotelId = hId)
        StaffRepository.addStaff(staff)
    }

    fun addNewItem(name: String, price: Double, unit: String, category: String, description: String, imageUrl: String) {
        val newItem = MenuItem(name = name, price = price, unit = unit, category = category, description = description, imageUrl = imageUrl)
        MenuRepository.addItem(newItem)
    }

    fun attendCall(callId: String) {
        CallRepository.attendCall(callId)
    }

    fun receiveCall(callId: String) {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("calls")
            .child(callId).child("status").setValue(CallStatus.RECEIVED)
    }

    fun markNotificationAsRead(id: String) {
        NotificationRepository.markAsRead(id)
    }

    fun markAllNotificationsAsRead() {
        NotificationRepository.markAllAsRead()
    }

    fun addBooking(booking: com.example.roomservice.data.model.Booking) {
        com.example.roomservice.data.BookingRepository.addBooking(booking)
    }

    fun updateBookingStatus(bookingId: String, status: com.example.roomservice.data.model.BookingStatus) {
        com.example.roomservice.data.BookingRepository.updateBookingStatus(bookingId, status)
    }

    fun checkInWithId(bookingId: String, idPhotoUrl: String) {
        com.example.roomservice.data.BookingRepository.checkInGuest(bookingId, idPhotoUrl)
    }

    fun deleteBooking(bookingId: String) {
        com.example.roomservice.data.BookingRepository.deleteBooking(bookingId)
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        OrderRepository.updateOrderStatus(orderId, newStatus)
    }

    // Shop Management
    fun addNewShopItem(name: String, price: Double, unit: String, category: String, description: String, imageUrl: String, stock: Int) {
        val newItem = MenuItem(name = name, price = price, unit = unit, category = category, description = description, imageUrl = imageUrl, stock = stock, itemType = "SHOP")
        com.example.roomservice.data.ShopRepository.addItem(newItem)
    }

    fun updateShopItem(item: MenuItem) {
        com.example.roomservice.data.ShopRepository.updateItem(item)
    }

    fun removeShopItem(itemId: String) {
        com.example.roomservice.data.ShopRepository.deleteItem(itemId)
    }

    fun toggleShopItemAvailability(itemId: String, currentStatus: Boolean) {
        com.example.roomservice.data.ShopRepository.toggleAvailability(itemId, !currentStatus)
    }

    fun updateShopStock(itemId: String, newStock: Int) {
        com.example.roomservice.data.ShopRepository.updateStock(itemId, newStock)
    }

    fun addNewShopCategory(name: String) {
        if (name.isNotBlank()) com.example.roomservice.data.ShopRepository.addCategory(name)
    }

    fun removeShopCategory(categoryId: String) {
        com.example.roomservice.data.ShopRepository.deleteCategory(categoryId)
    }
}
