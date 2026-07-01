package com.example.roomservice.data.model

import java.util.UUID

data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val bookingNumber: String = "",
    val hotelId: String = "",
    val roomNumber: String = "",
    val guestName: String = "",
    val guestPhone: String = "",
    val checkInDate: Long = 0L,
    val checkOutDate: Long = 0L,
    val totalAmount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val numberOfGuests: Int = 1,
    val guestIdentities: List<GuestIdentity> = emptyList(),
    val status: BookingStatus = BookingStatus.BOOKED,
    val bookingAgent: String = "Manual Booking",
    val discount: Double = 0.0,
    val isFullPay: Boolean = false,
    val upiTransactionId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class GuestIdentity(
    val idType: String? = null,
    val idNumber: String? = null,
    val frontPhotoUrl: String? = null,
    val backPhotoUrl: String? = null
)

enum class BookingStatus {
    BOOKED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED
}
