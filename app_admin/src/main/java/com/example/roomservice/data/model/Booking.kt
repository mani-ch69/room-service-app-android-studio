package com.example.roomservice.data.model

import java.util.UUID

data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val bookingNumber: String = "",
    val hotelId: String = "",
    val roomNumber: String = "",
    val roomType: String = "",
    val roomQuantity: Int = 1, // NEW: Number of rooms booked in this entry
    val guestName: String = "",
    val guestPhone: String = "",
    val checkInDate: Long = 0L,
    val checkOutDate: Long = 0L,
    val roomRent: Double = 0.0,
    val totalAmount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val paymentMode: String = "Cash",
    val numberOfGuests: Int = 1,
    val guestIdentities: List<GuestIdentity> = emptyList(),
    val status: BookingStatus = BookingStatus.BOOKED,
    val bookingAgent: String = "Manual Booking",
    val discount: Double = 0.0,
    val isFullPay: Boolean = false,
    val upiTransactionId: String = "",
    val receiptNumber: String = "",
    val receivedBy: String = "",
    val paymentType: String = "Advance",
    val paymentDate: Long = System.currentTimeMillis(),
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
