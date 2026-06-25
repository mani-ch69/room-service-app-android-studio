package com.example.roomservice.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import kotlinx.coroutines.delay

object HotelSession {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _hotelId = MutableStateFlow<String?>(null)
    val hotelId: StateFlow<String?> = _hotelId

    fun setHotelId(id: String?) {
        if (_hotelId.value == id) return // Avoid re-initialization if same ID
        _hotelId.value = id
        if (id != null) {
            scope.launch {
                // Initialize repositories with small delays to avoid UI thread contention
                OrderRepository.startListening(id)
                delay(50)
                CallRepository.startListening(id)
                delay(50)
                ChatRepository.startListening(id)
                delay(50)
                MenuRepository.startListening(id)
                delay(50)
                StaffRepository.startListening(id)
                delay(50)
                BusinessDetailsRepository.startListening(id)
                delay(50)
                RoomRepository.startListening(id)
                delay(50)
                BookingRepository.startListening(id)
                delay(50)
                NotificationRepository.startListening(id)
                delay(50)
                ShopRepository.startListening(id)
            }
        }
    }

    fun clear() {
        _hotelId.value = null
        OrderRepository.clearData()
        CallRepository.clearData()
        ChatRepository.clearData()
        MenuRepository.clearData()
        StaffRepository.clearData()
        BusinessDetailsRepository.clearData()
        RoomRepository.clearData()
        BookingRepository.clearData()
        NotificationRepository.clearData()
        // ShopRepository doesn't have a clearData yet, but good to add if needed
    }
}
