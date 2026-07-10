package com.example.roomservice.ui.waiter

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomservice.data.*
import com.example.roomservice.data.model.*
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RoomLiveStatus(
    val room: Room
)

data class DashboardStats(
    val totalBookings: Int = 0,
    val activeStays: Int = 0,
    val pendingArrivalsToday: Int = 0
)

class AdminMenuViewModel : ViewModel() {
    val notifications: StateFlow<List<Notification>> = NotificationRepository.notifications
    
    val unreadNotificationsCount: StateFlow<Int> = notifications.map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bookings: StateFlow<List<Booking>> = BookingRepository.bookings
    
    val dashboardStats: StateFlow<DashboardStats> = bookings.map { list ->
        val today = Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrow = today + 86400000L

        val active = list.count { it.status == BookingStatus.CHECKED_IN }
        val pendingToday = list.count { it.status == BookingStatus.BOOKED && it.checkInDate in today until tomorrow }
        
        DashboardStats(
            totalBookings = list.size,
            activeStays = active,
            pendingArrivalsToday = pendingToday
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

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

    fun addBooking(booking: Booking, context: Context? = null) {
        viewModelScope.launch {
            val finalBooking = if (booking.bookingNumber.isNullOrBlank()) {
                booking.copy(bookingNumber = (1000000000L..9999999999L).random().toString())
            } else booking
            BookingRepository.addBooking(finalBooking, context)
        }
    }

    fun updateBookingStatus(id: String, status: BookingStatus, context: Context? = null) {
        BookingRepository.updateBookingStatus(id, status, context)
    }

    fun deleteBooking(id: String) {
        BookingRepository.deleteBooking(id)
    }
}
