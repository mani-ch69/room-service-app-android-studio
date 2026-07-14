package com.example.roomservice.ui.waiter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.Room
import com.example.roomservice.ui.common.BookingCard
import com.example.roomservice.ui.common.WhatsAppTemplateDialog
import com.example.roomservice.ui.common.StatCard
import com.example.roomservice.ui.util.AuroraBackground
import java.util.*

@Composable
fun PaymentsScreen(bookings: List<Booking>, rooms: List<Room>) {
    var selectedRange by remember { mutableStateOf("Today") }
    var editingBooking by remember { mutableStateOf<Booking?>(null) }
    
    val filteredBookings = remember(bookings, selectedRange) {
        val now = Calendar.getInstance()
        val startOfToday = now.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        
        bookings.filter { b ->
            when (selectedRange) {
                "Today" -> b.timestamp >= startOfToday
                "Last 7 Days" -> b.timestamp >= (System.currentTimeMillis() - 7 * 86400000L)
                "Last 30 Days" -> b.timestamp >= (System.currentTimeMillis() - 30 * 86400000L)
                else -> true
            }
        }.sortedByDescending { it.timestamp }
    }

    val totalRevenue = filteredBookings.sumOf { it.totalAmount }
    val totalPaid = filteredBookings.sumOf { it.advancePaid }
    val totalOutstanding = filteredBookings.sumOf { it.totalAmount - it.advancePaid - it.discount }

    AuroraBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = when(selectedRange) { "Today" -> 0; "Last 7 Days" -> 1; "Last 30 Days" -> 2; else -> 3 },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                listOf("Today", "Last 7 Days", "Last 30 Days", "All Time").forEach { range ->
                    Tab(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        text = { Text(range, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedRange == range) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Revenue", "₹${totalRevenue.toInt()}", Icons.Default.TrendingUp, Color(0xFF64B5F6), Modifier.weight(1f))
                        StatCard("Collected", "₹${totalPaid.toInt()}", Icons.Default.CheckCircle, Color(0xFF81C784), Modifier.weight(1f))
                    }
                }
                
                item {
                    StatCard("Outstanding", "₹${totalOutstanding.toInt()}", Icons.Default.PendingActions, Color(0xFFE57373), Modifier.fillMaxWidth())
                }

                item {
                    Text(
                        text = "Recent Transactions", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 18.sp, 
                        color = MaterialTheme.colorScheme.onBackground, 
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = com.example.roomservice.ui.util.GlassTextStyle
                    )
                }

                if (filteredBookings.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(filteredBookings, key = { it.id }) { booking ->
                        var showWhatsAppDialog by remember { mutableStateOf(false) }
                        val businessDetails by com.example.roomservice.data.BusinessDetailsRepository.details.collectAsState()
                        val context = LocalContext.current

                        BookingCard(
                            booking = booking,
                            rooms = rooms,
                            onWhatsApp = { showWhatsAppDialog = true },
                            actionButton = {
                                Button(
                                    onClick = { editingBooking = booking },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Payment", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )

                        if (showWhatsAppDialog) {
                            WhatsAppTemplateDialog(
                                booking = booking,
                                business = businessDetails,
                                onDismiss = { showWhatsAppDialog = false },
                                onSend = { message ->
                                    com.example.roomservice.util.ReceiptHelper.sendWhatsAppMessage(context, booking.guestPhone, message)
                                    showWhatsAppDialog = false
                                }
                            )
                        }
                    }
                }
                
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
        
        if (editingBooking != null) {
            AddPaymentDialog(
                booking = editingBooking!!,
                onDismiss = { editingBooking = null },
                onConfirm = { updated ->
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("hotels")
                        .child(updated.hotelId).child("bookings").child(updated.id).setValue(updated)
                    editingBooking = null
                }
            )
        }
    }
}
