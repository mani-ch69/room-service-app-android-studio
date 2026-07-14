package com.example.roomservice.data

import android.content.Context
import android.content.SharedPreferences
import com.example.roomservice.data.model.WhatsAppTemplate

object WhatsAppTemplateRepository {
    private const val PREFS_NAME = "whatsapp_templates_prefs"
    
    private val defaultTemplates = listOf(
        WhatsAppTemplate("conf", "Booking Confirmation", "Hello {GuestName},\n\nYour booking at {HotelName} is confirmed!\n\nBooking ID: {BookingID}\nRoom: {RoomName}\nCheck-in: {CheckInDate}\nCheck-out: {CheckOutDate}\n\nWe look forward to hosting you!"),
        WhatsAppTemplate("adv", "Advance Payment Request", "Hello {GuestName},\n\nTo confirm your reservation {BookingID} at {HotelName}, kindly pay an advance of ₹{AdvanceAmount}.\n\nTotal Amount: ₹{BookingAmount}\nRemaining: ₹{RemainingAmount}\n\nThank you!"),
        WhatsAppTemplate("rem", "Check-in Reminder", "Hi {GuestName},\n\nThis is a friendly reminder for your check-in today at {HotelName}.\n\nAddress: {HotelAddress}\nMap: {GoogleMapLink}\n\nSee you soon!"),
        WhatsAppTemplate("loc", "Location & Directions", "Hello {GuestName},\n\nHere are the directions to {HotelName}:\n\nAddress: {HotelAddress}\nGoogle Maps: {GoogleMapLink}\n\nSafe travels!"),
        WhatsAppTemplate("wifi", "Wi-Fi Details", "Welcome {GuestName}!\n\nHere are the Wi-Fi details for {HotelName}:\n\nNetwork: {HotelName}_Guest\nPassword: guest@123\n\nEnjoy your stay!"),
        WhatsAppTemplate("thanks", "Thank You Message", "Hello {GuestName},\n\nThank you for choosing {HotelName}. We hope you had a pleasant stay with us.\n\nHope to see you again soon!"),
        WhatsAppTemplate("rev", "Review Request", "Hi {GuestName},\n\nWe would love to hear about your experience at {HotelName}. Please share your review here: {ReviewLink}\n\nThank you!"),
        WhatsAppTemplate("custom", "Custom Template", "Hello {GuestName},\n\n{HotelName} wishes you a great day!")
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTemplates(context: Context): List<WhatsAppTemplate> {
        val prefs = getPrefs(context)
        return defaultTemplates.map { default ->
            val content = prefs.getString("tpl_${default.id}", default.content) ?: default.content
            default.copy(content = content)
        }
    }

    fun updateTemplate(context: Context, updated: WhatsAppTemplate) {
        getPrefs(context).edit().putString("tpl_${updated.id}", updated.content).apply()
    }

    fun resetToDefault(context: Context) {
        val editor = getPrefs(context).edit()
        defaultTemplates.forEach { 
            editor.remove("tpl_${it.id}")
        }
        editor.apply()
    }
}
