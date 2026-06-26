package com.example.roomservice.ui.waiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomservice.data.*
import com.example.roomservice.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RoomLiveStatus(
    val room: Room
)

class AdminMenuViewModel : ViewModel() {
    val notifications: StateFlow<List<Notification>> = NotificationRepository.notifications
    
    val unreadNotificationsCount: StateFlow<Int> = notifications.map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bookings: StateFlow<List<Booking>> = BookingRepository.bookings
    
    private val _notificationSignal = MutableSharedFlow<String>()
    val notificationSignal: SharedFlow<String> = _notificationSignal.asSharedFlow()

    val roomLiveStatuses: StateFlow<List<RoomLiveStatus>> = RoomRepository.rooms
        .map { rooms ->
            rooms.map { room ->
                RoomLiveStatus(room = room)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        HotelSession.hotelId.onEach { id ->
            if (id != null) {
                RoomRepository.startListening(id)
                BookingRepository.startListening(id)
                NotificationRepository.startListening(id)
                BusinessDetailsRepository.startListening(id)
            }
        }.launchIn(viewModelScope)
    }

    fun markNotificationAsRead(id: String) { NotificationRepository.markAsRead(id) }
    fun markAllNotificationsAsRead() { NotificationRepository.markAllAsRead() }

    fun addBooking(booking: Booking) {
        viewModelScope.launch {
            BookingRepository.addBooking(booking)
        }
    }

    fun updateBookingStatus(id: String, status: BookingStatus) {
        BookingRepository.updateBookingStatus(id, status)
    }

    fun deleteBooking(id: String) {
        BookingRepository.deleteBooking(id)
    }
}
