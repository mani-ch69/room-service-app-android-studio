package com.example.roomservice.util

import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BookingStatus
import com.example.roomservice.ui.common.DateRangeUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object CalendarSyncHelper {

    fun syncFromIcal(
        hotelId: String, 
        roomNumber: String, 
        roomType: String, 
        provider: String,
        icalUrl: String, 
        onComplete: (List<Booking>) -> Unit
    ) {
        Thread {
            try {
                val url = URL(icalUrl)
                val connection = url.openConnection() as HttpURLConnection
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                
                val bookings = mutableListOf<Booking>()
                var currentLine: String?
                var currentBooking: Booking? = null
                
                // Formats for iCal: DateTime (20240710T120000Z) or Date (20240710)
                val sdfDateTime = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                val sdfDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }

                while (reader.readLine().also { currentLine = it } != null) {
                    val line = currentLine ?: ""
                    when {
                        line.startsWith("BEGIN:VEVENT") -> {
                            currentBooking = Booking(
                                hotelId = hotelId,
                                roomNumber = roomNumber,
                                bookingAgent = provider,
                                status = BookingStatus.BOOKED
                            )
                        }
                        line.startsWith("UID:") -> {
                            currentBooking = currentBooking?.copy(bookingNumber = line.substringAfter("UID:").trim())
                        }
                        line.contains("DTSTART") -> {
                            val rawValue = line.substringAfter(":").trim()
                            val date = if (rawValue.contains("T")) sdfDateTime.parse(rawValue.substring(0, 15)) else sdfDate.parse(rawValue)
                            date?.let { 
                                currentBooking = currentBooking?.copy(checkInDate = DateRangeUtils.getNoonTimestamp(it.time))
                            }
                        }
                        line.contains("DTEND") -> {
                            val rawValue = line.substringAfter(":").trim()
                            val date = if (rawValue.contains("T")) sdfDateTime.parse(rawValue.substring(0, 15)) else sdfDate.parse(rawValue)
                            date?.let { 
                                currentBooking = currentBooking?.copy(checkOutDate = DateRangeUtils.getNoonTimestamp(it.time))
                            }
                        }
                        line.startsWith("SUMMARY:") -> {
                            currentBooking = currentBooking?.copy(guestName = line.substringAfter("SUMMARY:").trim())
                        }
                        line.startsWith("END:VEVENT") -> {
                            currentBooking?.let { 
                                // Only add if dates are valid
                                if (it.checkInDate > 0 && it.checkOutDate > 0) {
                                    bookings.add(it)
                                }
                            }
                            currentBooking = null
                        }
                    }
                }
                onComplete(bookings)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(emptyList())
            }
        }.start()
    }

    fun updateFirebaseWithBookings(hotelId: String, bookings: List<Booking>) {
        if (bookings.isEmpty()) return
        
        val db = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("hotels")
            .child(hotelId).child("bookings")
            
        bookings.forEach { booking ->
            // Use bookingNumber (which is UID from iCal) as key to avoid duplicates
            // Strip special chars from UID if needed
            val sanitizedKey = booking.bookingNumber.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
            
            db.child(sanitizedKey).get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    db.child(sanitizedKey).setValue(booking.copy(id = sanitizedKey))
                }
            }
        }
    }
}
