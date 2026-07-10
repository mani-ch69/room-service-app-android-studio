package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaymentsScreen(bookings: List<Booking>) {
    var selectedRange by remember { mutableStateOf("Today") }
    
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        // Range Selector
        ScrollableTabRow(
            selectedTabIndex = when(selectedRange) { "Today" -> 0; "Last 7 Days" -> 1; "Last 30 Days" -> 2; else -> 3 },
            containerColor = Color.White,
            contentColor = Color(0xFF1976D2),
            edgePadding = 16.dp,
            divider = {}
        ) {
            listOf("Today", "Last 7 Days", "Last 30 Days", "All Time").forEach { range ->
                Tab(
                    selected = selectedRange == range,
                    onClick = { selectedRange = range },
                    text = { Text(range, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentSummaryCard("Revenue", "₹${totalRevenue.toInt()}", Icons.Default.TrendingUp, Color(0xFF1976D2), Modifier.weight(1f))
                    PaymentSummaryCard("Collected", "₹${totalPaid.toInt()}", Icons.Default.CheckCircle, Color(0xFF2E7D32), Modifier.weight(1f))
                }
            }
            
            item {
                PaymentSummaryCard("Outstanding", "₹${totalOutstanding.toInt()}", Icons.Default.PendingActions, Color.Red, Modifier.fillMaxWidth())
            }

            item {
                Text("Recent Transactions", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))
            }

            if (filteredBookings.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions found", color = Color.Gray)
                    }
                }
            } else {
                items(filteredBookings) { booking ->
                    TransactionItem(booking)
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun PaymentSummaryCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun TransactionItem(booking: Booking) {
    val sdf = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val outstanding = booking.totalAmount - booking.advancePaid - booking.discount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(booking.guestName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(sdf.format(Date(booking.timestamp)), fontSize = 11.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Surface(
                        color = when(booking.paymentMode) { "Cash" -> Color(0xFFF1F5F9); "UPI" -> Color(0xFFE3F2FD); else -> Color(0xFFF1F5F9) },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            booking.paymentMode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = if(booking.paymentMode == "UPI") Color(0xFF1976D2) else Color.DarkGray
                        )
                    }
                    if (booking.upiTransactionId.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text("Ref: ${booking.upiTransactionId}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("₹${booking.totalAmount.toInt()}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                if (outstanding > 0) {
                    Text("Due: ₹${outstanding.toInt()}", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Paid", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
