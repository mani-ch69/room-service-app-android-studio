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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceMiniCard(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
fun StatusBadge(isCheckIn: Boolean, isCheckOut: Boolean, isStayOver: Boolean) {
    val (bg, txt, label) = when {
        isCheckIn -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Check-in")
        isCheckOut -> Triple(Color(0xFFFFEBEE), Color.Red, "Check-out")
        isStayOver -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "Stay over")
        else -> Triple(Color.Transparent, Color.Transparent, "")
    }
    
    if (label.isNotEmpty()) {
        Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
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
fun BookingCard(
    booking: Booking,
    rooms: List<Room>,
    selectedDate: Long? = null,
    onEdit: (() -> Unit)? = null,
    onStatusClick: (() -> Unit)? = null,
    onPrint: (() -> Unit)? = null,
    onWhatsApp: (() -> Unit)? = null,
    onPhoneClick: (() -> Unit)? = null,
    actionButton: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val df = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val checkInStr = remember(booking.checkInDate) { df.format(Date(booking.checkInDate)) }
    val checkOutStr = remember(booking.checkOutDate) { df.format(Date(booking.checkOutDate)) }
    
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

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.guestName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.Black,
                        maxLines = 1
                    )
                    
                    Text(
                        text = "Booking ID: ${if (!booking.bookingNumber.isNullOrBlank()) booking.bookingNumber else "Pending..."}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    
                    Text(
                        text = "Booked on: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(booking.timestamp))}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                        }
                    }
                    if (onPrint != null) {
                        IconButton(onClick = onPrint, modifier = Modifier.size(36.dp)) { 
                            Icon(Icons.Default.Print, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) 
                        }
                    }
                    if (onWhatsApp != null) {
                        IconButton(onClick = onWhatsApp, modifier = Modifier.size(36.dp)) { 
                            Icon(painter = painterResource(id = R.drawable.ic_whatsapp), null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (booking.guestPhone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                if (onPhoneClick != null) onPhoneClick()
                                else {
                                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL).apply { 
                                        data = android.net.Uri.parse("tel:${booking.guestPhone}") 
                                    })
                                }
                            }
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Phone, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = booking.guestPhone, fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                Text(
                    text = "$checkInStr - $checkOutStr",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = "$nights nights • ${booking.numberOfGuests} guests", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PriceMiniCard("Total", "₹${booking.totalAmount.toInt()}", Color.Black)
                        PriceMiniCard("Paid", "₹${booking.advancePaid.toInt()}", Color(0xFF2E7D32))
                        PriceMiniCard("Pending", "₹${outstanding.toInt()}", Color.Red)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(isCheckIn, isCheckOut, isStayOver)
                    
                    if (onStatusClick != null) {
                        Spacer(Modifier.height(8.dp))
                        val (statusColor, statusLabel) = when(booking.status) {
                            BookingStatus.BOOKED -> Color(0xFF1976D2) to "OK"
                            BookingStatus.CHECKED_IN -> Color(0xFF2E7D32) to "ACTIVE"
                            BookingStatus.COMPLETED -> Color(0xFF64748B) to "OK"
                            BookingStatus.CANCELLED -> Color.Red to "CANCELLED"
                        }
                        
                        Surface(
                            onClick = onStatusClick,
                            color = statusColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, statusColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusLabel,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, null, tint = statusColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (roomType.isNotEmpty()) {
                        Text(
                            text = roomType,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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

fun isSameDay(c1: Calendar, c2: Calendar) = 
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && 
    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
