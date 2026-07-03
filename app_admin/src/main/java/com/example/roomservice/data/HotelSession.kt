package com.example.roomservice.data

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
        if (_hotelId.value == id) return 
        _hotelId.value = id
        if (id != null) {
            scope.launch {
                // Only keep existing and relevant repositories
                BusinessDetailsRepository.startListening(id)
                delay(50)
                RoomRepository.startListening(id)
                delay(50)
                BookingRepository.startListening(id)
                delay(50)
                NotificationRepository.startListening(id)
            }
        }
    }

    fun clear() {
        _hotelId.value = null
        BusinessDetailsRepository.clearData()
        RoomRepository.clearData()
        BookingRepository.clearData()
        NotificationRepository.clearData()
    }
}
