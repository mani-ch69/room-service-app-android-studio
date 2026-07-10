package com.example.roomservice.util

import android.content.Context
import com.example.roomservice.data.model.Booking
import com.google.gson.Gson
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GoogleSheetsHelper {

    fun saveSheetUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("google_sheet_url", url).apply()
    }

    fun getSheetUrl(context: Context): String? {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getString("google_sheet_url", null)
    }

    fun syncBookingToSheet(context: Context, booking: Booking) {
        val scriptUrl = getSheetUrl(context) ?: return
        if (scriptUrl.isBlank()) return

        Thread {
            try {
                val url = URL(scriptUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val payload = mapOf(
                    "action" to "sync",
                    "booking" to booking
                )
                val json = Gson().toJson(payload)

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(json)
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                // 302 Redirect is common for Apps Script Web Apps
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    val newUrl = connection.getHeaderField("Location")
                    val newConn = URL(newUrl).openConnection() as HttpURLConnection
                    newConn.responseCode // Just to trigger the redirected request
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
