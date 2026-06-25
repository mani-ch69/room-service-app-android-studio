package com.example.roomservice.data

import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BookingStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BookingRepository {
    private var db = FirebaseDatabase.getInstance().getReference("bookings")
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()
    private var listener: ValueEventListener? = null

    fun startListening(hotelId: String) {
        stopListening()
        db = FirebaseDatabase.getInstance().getReference("hotels").child(hotelId).child("bookings")
        db.keepSynced(true)
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Booking::class.java) }
                _bookings.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(listener!!)
    }

    private fun stopListening() {
        listener?.let { db.removeEventListener(it) }
        listener = null
    }

    fun addBooking(booking: Booking) {
        db.child(booking.id).setValue(booking)
    }

    fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        db.child(bookingId).child("status").setValue(status)
    }

    fun checkInGuest(bookingId: String, idPhotoUrl: String) {
        db.child(bookingId).updateChildren(mapOf(
            "status" to BookingStatus.CHECKED_IN,
            "guestIdPhotoUrl" to idPhotoUrl
        ))
    }

    fun deleteBooking(bookingId: String) {
        db.child(bookingId).removeValue()
    }

    fun clearData() {
        stopListening()
        _bookings.value = emptyList()
    }
}
