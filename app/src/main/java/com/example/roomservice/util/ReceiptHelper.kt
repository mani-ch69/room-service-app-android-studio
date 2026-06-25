package com.example.roomservice.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BusinessDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptHelper {

    fun shareReceiptOnWhatsApp(context: Context, booking: Booking, business: BusinessDetails, roomType: String) {
        val message = generateWhatsAppMessage(booking, business, roomType)
        val phoneNumber = booking.guestPhone.replace(" ", "").replace("+", "")
        
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
            intent.data = Uri.parse(url)
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share if WhatsApp is not installed
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, message)
            context.startActivity(Intent.createChooser(intent, "Share Receipt"))
        }
    }

    private fun generateWhatsAppMessage(booking: Booking, business: BusinessDetails, roomType: String): String {
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = df.format(Date())
        val checkIn = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(booking.checkInDate))
        val checkOut = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(booking.checkOutDate))

        val gst = booking.totalAmount * 0.12
        val subtotal = booking.totalAmount - gst
        val outstanding = booking.totalAmount - booking.advancePaid

        return """
            *${business.hotelName.uppercase()}*
            ${business.address}
            Contact: ${business.phone}
            
            *BOOKING RECEIPT*
            --------------------------
            Room No: *${booking.roomNumber}*
            Room Type: $roomType
            Booking ID: ${booking.bookingNumber}
            Guest: ${booking.guestName}
            
            Check-In: $checkIn
            Check-Out: $checkOut
            --------------------------
            Room Charges: ₹${String.format(Locale.getDefault(), "%.2f", subtotal)}
            GST (12%): ₹${String.format(Locale.getDefault(), "%.2f", gst)}
            *Total: ₹${String.format(Locale.getDefault(), "%.2f", booking.totalAmount)}*
            
            Advance Paid: ₹${String.format(Locale.getDefault(), "%.2f", booking.advancePaid)}
            *Outstanding: ₹${String.format(Locale.getDefault(), "%.2f", outstanding)}*
            --------------------------
            Date: $dateStr
            
            Thank you for staying with us!
        """.trimIndent()
    }

    fun printBookingReceipt(context: Context, booking: Booking, business: BusinessDetails, roomType: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Booking_${booking.bookingNumber}")
                val jobName = "RoomService_Receipt_${booking.roomNumber}"
                
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.NA_INDEX_3X5)
                    .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                    .build()
                
                printManager.print(jobName, printAdapter, printAttributes)
            }
        }

        val html = generateReceiptHtml(booking, business, roomType)
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun generateReceiptHtml(booking: Booking, business: BusinessDetails, roomType: String): String {
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = df.format(Date())
        val checkIn = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(booking.checkInDate))
        val checkOut = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(booking.checkOutDate))

        val gst = booking.totalAmount * 0.12 // Simulation: 12% GST
        val subtotal = booking.totalAmount - gst
        val outstanding = booking.totalAmount - booking.advancePaid

        return """
            <html>
            <head>
                <style>
                    body { font-family: 'Courier New', Courier, monospace; width: 300px; padding: 10px; margin: 0; }
                    .header { text-align: center; border-bottom: 1px dashed #000; padding-bottom: 10px; }
                    .hotel-name { font-size: 20px; font-weight: bold; text-transform: uppercase; }
                    .info-line { display: flex; justify-content: space-between; margin: 5px 0; font-size: 14px; }
                    .divider { border-top: 1px dashed #000; margin: 10px 0; }
                    .total { font-size: 18px; font-weight: bold; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="hotel-name">${business.hotelName}</div>
                    <div>${business.address}</div>
                    <div>Phone: ${business.phone}</div>
                    <div>GSTIN: ${business.gstNumber}</div>
                </div>

                <div class="divider"></div>
                <div style="text-align:center; font-weight:bold;">BOOKING RECEIPT</div>
                <div class="divider"></div>

                <div class="info-line"><span>Room No:</span> <b>${booking.roomNumber}</b></div>
                <div class="info-line"><span>Room Type:</span> <b>$roomType</b></div>
                <div class="info-line"><span>Booking ID:</span> <b>${booking.bookingNumber}</b></div>
                <div class="info-line"><span>Guest:</span> <b>${booking.guestName}</b></div>
                <div class="info-line"><span>Guests Count:</span> <b>${booking.numberOfGuests}</b></div>
                
                <div class="divider"></div>
                
                <div class="info-line"><span>Check-In:</span> <b>$checkIn</b></div>
                <div class="info-line"><span>Check-Out:</span> <b>$checkOut</b></div>
                
                <div class="divider"></div>

                <div class="info-line"><span>Room Charges:</span> <span>₹${String.format("%.2f", subtotal)}</span></div>
                <div class="info-line"><span>GST (12%):</span> <span>₹${String.format("%.2f", gst)}</span></div>
                <div class="info-line total"><span>Total:</span> <span>₹${String.format("%.2f", booking.totalAmount)}</span></div>
                
                <div class="divider"></div>
                
                <div class="info-line"><span>Advance Paid:</span> <span>₹${String.format("%.2f", booking.advancePaid)}</span></div>
                <div class="info-line"><span>Outstanding:</span> <b style="color:red;">₹${String.format("%.2f", outstanding)}</b></div>

                <div class="divider"></div>
                <div class="footer">
                    <div>Date: $dateStr</div>
                    <p>Thank you for choosing us!</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
