package com.example.roomservice.util

import android.content.Context
import com.example.roomservice.data.RoomRepository
import com.example.roomservice.data.model.Booking
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleSheetsHelper {

    fun saveSheetUrls(context: Context, urls: List<String>) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("google_sheet_urls", urls.toSet()).apply()
    }

    fun getSheetUrls(context: Context): List<String> {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val urls = prefs.getStringSet("google_sheet_urls", null)?.toList()
        
        if (urls == null) {
            // Check for legacy single URL
            val legacyUrl = prefs.getString("google_sheet_url", null)
            if (!legacyUrl.isNullOrBlank()) {
                val newList = listOf(legacyUrl)
                saveSheetUrls(context, newList)
                return newList
            }
            return emptyList()
        }
        return urls
    }

    fun addSheetUrl(context: Context, url: String) {
        val current = getSheetUrls(context).toMutableList()
        if (!current.contains(url)) {
            current.add(url)
            saveSheetUrls(context, current)
        }
    }

    fun removeSheetUrl(context: Context, url: String) {
        val current = getSheetUrls(context).toMutableList()
        if (current.remove(url)) {
            saveSheetUrls(context, current)
        }
    }

    fun syncBookingToSheet(context: Context, booking: Booking, onComplete: ((Boolean) -> Unit)? = null) {
        val scriptUrls = getSheetUrls(context)
        if (scriptUrls.isEmpty()) return

        Thread {
            try {
                val remaining = booking.totalAmount - booking.advancePaid - booking.discount
                
                // Get Room Type from Repository if available
                val rooms = RoomRepository.rooms.value
                val roomInfo = rooms.find { it.roomNumber == booking.roomNumber }
                val roomType = roomInfo?.roomType ?: "N/A"
                val roomUnit = roomInfo?.totalUnits?.toString() ?: "1"

                val sdfDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val now = Date()
                val syncDate = sdfDate.format(now)
                val syncTime = sdfTime.format(now)
                val bookingDate = sdfDate.format(Date(booking.timestamp))

                val json = """
                    {
                      "action": "sync",
                      "booking": {
                        "bookingNumber": "${booking.bookingNumber}",
                        "bookingDate": "$bookingDate",
                        "guestName": "${booking.guestName}",
                        "guestPhone": "${booking.guestPhone}",
                        "roomType": "${booking.roomType}",
                        "roomQuantity": ${booking.roomQuantity},
                        "roomNumber": "${booking.roomNumber}",
                        "checkInDate": ${booking.checkInDate},
                        "checkOutDate": ${booking.checkOutDate},
                        "totalAmount": ${booking.totalAmount},
                        "advancePaid": ${booking.advancePaid},
                        "remainingAmount": $remaining,
                        "bookingAgent": "${booking.bookingAgent}",
                        "paymentMode": "${booking.paymentMode}",
                        "status": "${booking.status}",
                        "syncDate": "$syncDate",
                        "syncTime": "$syncTime",
                        "timestamp": ${booking.timestamp}
                      }
                    }
                """.trimIndent()

                var allSuccess = true
                scriptUrls.forEach { url ->
                    if (!postData(url, json)) allSuccess = false
                }
                onComplete?.invoke(allSuccess)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete?.invoke(false)
            }
        }.start()
    }

    private fun postData(targetUrl: String, jsonData: String): Boolean {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(targetUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.instanceFollowRedirects = false // Manually follow redirects for POST
            connection.setRequestProperty("Content-Type", "application/json")

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonData)
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            
            // Google Apps Script always redirects (302) a POST request
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == 307) {
                val newUrl = connection.getHeaderField("Location")
                return postData(newUrl, jsonData) // Follow the redirect with original data
            }

            return responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            connection?.disconnect()
        }
    }

    fun syncAllBookings(
        context: Context, 
        bookings: List<Booking>, 
        specificUrl: String? = null,
        onProgress: (Int, Int) -> Unit, 
        onComplete: (Boolean) -> Unit
    ) {
        if (bookings.isEmpty()) {
            onComplete(true)
            return
        }

        val urls = if (specificUrl != null) listOf(specificUrl) else getSheetUrls(context)
        if (urls.isEmpty()) {
            onComplete(false)
            return
        }

        Thread {
            var syncedCount = 0
            var allSuccess = true
            
            val rooms = RoomRepository.rooms.value
            val sdfDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            bookings.forEach { booking ->
                val remaining = booking.totalAmount - booking.advancePaid - booking.discount
                val roomInfo = rooms.find { it.roomNumber == booking.roomNumber }
                val roomType = roomInfo?.roomType ?: "N/A"
                val roomUnit = roomInfo?.totalUnits?.toString() ?: "1"
                
                val now = Date()
                val syncDate = sdfDate.format(now)
                val syncTime = sdfTime.format(now)
                val bookingDate = sdfDate.format(Date(booking.timestamp))

                val json = """
                    {
                      "action": "sync",
                      "booking": {
                        "bookingNumber": "${booking.bookingNumber}",
                        "bookingDate": "$bookingDate",
                        "guestName": "${booking.guestName}",
                        "guestPhone": "${booking.guestPhone}",
                        "roomType": "${booking.roomType}",
                        "roomQuantity": ${booking.roomQuantity},
                        "roomNumber": "${booking.roomNumber}",
                        "checkInDate": ${booking.checkInDate},
                        "checkOutDate": ${booking.checkOutDate},
                        "totalAmount": ${booking.totalAmount},
                        "advancePaid": ${booking.advancePaid},
                        "remainingAmount": $remaining,
                        "bookingAgent": "${booking.bookingAgent}",
                        "paymentMode": "${booking.paymentMode}",
                        "status": "${booking.status}",
                        "syncDate": "$syncDate",
                        "syncTime": "$syncTime",
                        "timestamp": ${booking.timestamp}
                      }
                    }
                """.trimIndent()

                urls.forEach { url ->
                    if (!postData(url, json)) allSuccess = false
                }
                
                syncedCount++
                onProgress(syncedCount, bookings.size)
                
                // Small delay to prevent hitting Google's rate limits
                Thread.sleep(400)
            }
            onComplete(allSuccess)
        }.start()
    }
}
