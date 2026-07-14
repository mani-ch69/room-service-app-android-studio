package com.example.roomservice.util

import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BusinessDetails
import java.text.SimpleDateFormat
import java.util.*

object TemplateEngine {
    val placeholders = listOf(
        "{GuestName}", "{Mobile}", "{BookingID}", "{RoomName}", "{RoomNumber}",
        "{CheckInDate}", "{CheckOutDate}", "{CheckInTime}", "{CheckOutTime}",
        "{BookingAmount}", "{AdvanceAmount}", "{RemainingAmount}",
        "{HotelName}", "{HotelAddress}", "{GoogleMapLink}", "{ReviewLink}"
    )

    fun resolve(template: String, booking: Booking, business: BusinessDetails?): String {
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        
        val outstanding = booking.totalAmount - booking.advancePaid - booking.discount

        val replacements = mapOf(
            "{GuestName}" to booking.guestName,
            "{Mobile}" to booking.guestPhone,
            "{BookingID}" to booking.bookingNumber,
            "{RoomName}" to booking.roomType, 
            "{RoomNumber}" to booking.roomNumber,
            "{CheckInDate}" to df.format(Date(booking.checkInDate)),
            "{CheckOutDate}" to df.format(Date(booking.checkOutDate)),
            "{CheckInTime}" to (business?.checkInTime ?: "12:00 PM"),
            "{CheckOutTime}" to (business?.checkOutTime ?: "11:00 AM"),
            "{BookingAmount}" to booking.totalAmount.toString(),
            "{AdvanceAmount}" to booking.advancePaid.toString(),
            "{RemainingAmount}" to outstanding.toString(),
            "{HotelName}" to (business?.hotelName ?: "Our Hotel"),
            "{HotelAddress}" to (business?.address ?: ""),
            "{GoogleMapLink}" to "https://maps.google.com/?q=${business?.address ?: "Hotel"}",
            "{ReviewLink}" to "https://g.page/r/your-review-link"
        )

        var resolved = template
        replacements.forEach { (key, value) ->
            resolved = resolved.replace(key, value)
        }
        return resolved
    }
}
