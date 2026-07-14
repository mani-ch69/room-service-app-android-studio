package com.example.roomservice.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.R
import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BookingStatus
import com.example.roomservice.data.model.Room
import com.example.roomservice.ui.util.GlassCard
import com.example.roomservice.ui.util.GlassTextStyle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceMiniCard(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label, 
            fontSize = 10.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontWeight = FontWeight.Bold,
            style = GlassTextStyle
        )
        Text(
            text = value, 
            fontSize = 13.sp, 
            fontWeight = FontWeight.ExtraBold, 
            color = if (color == Color.White) MaterialTheme.colorScheme.onSurface else color,
            style = GlassTextStyle
        )
    }
}

@Composable
fun StatusBadge(isCheckIn: Boolean, isCheckOut: Boolean, isStayOver: Boolean) {
    val (bg, txt, label) = when {
        isCheckIn -> Triple(Color(0xFF1976D2).copy(alpha = 0.1f), Color(0xFF1976D2), "Check-in")
        isCheckOut -> Triple(Color(0xFF64748B).copy(alpha = 0.1f), Color(0xFF64748B), "Check-out")
        isStayOver -> Triple(Color(0xFF1976D2).copy(alpha = 0.1f), Color(0xFF1976D2), "Stay over")
        else -> Triple(Color.Transparent, Color.Transparent, "")
    }
    
    if (label.isNotEmpty()) {
        Surface(
            color = bg, 
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, txt.copy(alpha = 0.3f))
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = txt,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label, 
                    fontSize = 11.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontWeight = FontWeight.Bold,
                    style = GlassTextStyle
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Black, 
                color = MaterialTheme.colorScheme.onSurface,
                style = GlassTextStyle
            )
        }
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    rooms: List<Room>,
    selectedDate: Long? = null,
    onEdit: (() -> Unit)? = null,
    onStatusClick: (() -> Unit)? = null,
    onPrint: (() -> Unit)? = null,
    onWhatsApp: (() -> Unit)? = null,
    onWhatsAppReceipt: (() -> Unit)? = null,
    onWhatsAppContact: (() -> Unit)? = null,
    onPhoneClick: (() -> Unit)? = null,
    actionButton: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    val roomType = remember(booking.roomNumber, rooms) {
        rooms.find { it.roomNumber == booking.roomNumber }?.roomType ?: ""
    }

    val outstanding = booking.totalAmount - booking.advancePaid - booking.discount
    val nights = remember(booking.checkInDate, booking.checkOutDate) {
        val n = ((booking.checkOutDate - booking.checkInDate) / 86400000L).toInt()
        if (n <= 0) 1 else n
    }
    
    val referenceDate = selectedDate ?: System.currentTimeMillis()
    val isCheckIn = isSameDay(Calendar.getInstance().apply { timeInMillis = booking.checkInDate }, Calendar.getInstance().apply { timeInMillis = referenceDate })
    val isCheckOut = isSameDay(Calendar.getInstance().apply { timeInMillis = booking.checkOutDate }, Calendar.getInstance().apply { timeInMillis = referenceDate })
    val isStayOver = !isCheckIn && !isCheckOut && referenceDate > booking.checkInDate && referenceDate < booking.checkOutDate

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            
            // --- HEADER: BOOKING DATE & ID ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Booking Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(text = df.format(Date(booking.timestamp)), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = "#${if (!booking.bookingNumber.isNullOrBlank()) booking.bookingNumber else "Pending"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // --- SECTION 1: STAY DETAILS (Order as in Form) ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DetailColumn("Check-in", df.format(Date(booking.checkInDate)))
                DetailColumn("Check-out", df.format(Date(booking.checkOutDate)))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DetailColumn("Nights", "$nights Night${if(nights>1) "s" else ""}")
                DetailColumn("Room Type", roomType)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DetailColumn("Adults", "${booking.numberOfGuests} Guests")
                DetailColumn("Children", "0")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // --- SECTION 2: GUEST DETAILS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Guest Name", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(text = booking.guestName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Guest Mobile", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL).apply { 
                            data = android.net.Uri.parse("tel:${booking.guestPhone}") 
                        })
                    }) {
                        Icon(Icons.Default.Phone, null, tint = Color(0xFF1976D2), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = booking.guestPhone, fontSize = 14.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // --- SECTION 3: PAYMENT DETAILS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PriceMiniCard("Room Price", "₹${booking.roomRent.toInt()}/night", MaterialTheme.colorScheme.onSurface)
                PriceMiniCard("Discount", "₹${booking.discount.toInt()}", Color(0xFFD32F2F))
                PriceMiniCard("Advance Paid", "₹${booking.advancePaid.toInt()}", Color(0xFF2E7D32))
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    PriceMiniCard("Total Amount", "₹${booking.totalAmount.toInt()}", MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    PriceMiniCard("Remaining to Pay", "₹${outstanding.toInt()}", if(outstanding > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32))
                }
                StatusBadge(isCheckIn, isCheckOut, isStayOver)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // --- SECTION 4: ID PROOF & ACTIONS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ID Proof (${booking.guestIdentities.firstOrNull()?.idType ?: "ID"})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(text = booking.guestIdentities.firstOrNull()?.idNumber ?: "Not Provided", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onEdit != null) { IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) } }
                    if (onPrint != null) { IconButton(onClick = onPrint, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Print, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    if (onWhatsApp != null) { IconButton(onClick = onWhatsApp, modifier = Modifier.size(36.dp)) { Icon(painter = painterResource(id = R.drawable.ic_whatsapp), null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp)) } }
                    if (onWhatsAppReceipt != null) { IconButton(onClick = onWhatsAppReceipt, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ReceiptLong, null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp)) } }
                    if (onWhatsAppContact != null) { IconButton(onClick = onWhatsAppContact, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Chat, null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp)) } }
                }
            }
            
            // --- SECTION 5: STATUS BUTTON ---
            if (onStatusClick != null) {
                Spacer(Modifier.height(12.dp))
                val (statusColor, statusLabel) = when(booking.status) {
                    BookingStatus.BOOKED -> MaterialTheme.colorScheme.primary to "OK"
                    BookingStatus.CHECKED_IN -> Color(0xFF1976D2) to "ACTIVE"
                    BookingStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant to "OK"
                    BookingStatus.CANCELLED -> Color(0xFFD32F2F) to "CANCELLED"
                }
                Surface(
                    onClick = onStatusClick,
                    modifier = Modifier.fillMaxWidth(),
                    color = statusColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "STATUS: $statusLabel", color = statusColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Icon(Icons.Default.ArrowDropDown, null, tint = statusColor)
                    }
                }
            }

            if (actionButton != null) {
                Spacer(Modifier.height(16.dp))
                actionButton()
            }
        }
    }
}

@Composable
fun DetailColumn(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
    }
}

fun isSameDay(c1: Calendar, c2: Calendar) = 
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && 
    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
