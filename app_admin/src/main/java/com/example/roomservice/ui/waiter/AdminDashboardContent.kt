package com.example.roomservice.ui.waiter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roomservice.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardContent(
    roomStatuses: List<RoomLiveStatus>,
    bookings: List<Booking> = emptyList(),
    onAddBooking: (Booking) -> Unit = {},
    onDeleteBooking: (String) -> Unit = {}
) {
    val rooms = remember(roomStatuses) { roomStatuses.map { it.room } }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val pullToRefreshState = rememberPullToRefreshState()

    // Pull to Refresh logic
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            isRefreshing = true
            delay(1500) // Simulate sync
            isRefreshing = false
            pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFF1F5F9))
        .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        DashboardCalendarView(
            rooms = rooms, 
            bookings = bookings, 
            onAddBooking = onAddBooking,
            onDeleteBooking = onDeleteBooking
        )

        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DashboardCalendarView(
    rooms: List<Room>,
    bookings: List<Booking>,
    onAddBooking: (Booking) -> Unit,
    onDeleteBooking: (String) -> Unit
) {
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val sdfMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    // Optimization: Filter bookings only when bookings or selectedDate changes
    val bookingsForDate by remember(bookings, selectedDate) {
        derivedStateOf {
            bookings.filter { b ->
                val checkIn = Calendar.getInstance().apply { timeInMillis = b.checkInDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                val checkOut = Calendar.getInstance().apply { timeInMillis = b.checkOutDate; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
                
                val sel = Calendar.getInstance().apply { timeInMillis = selectedDate; set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }.timeInMillis
                sel >= checkIn && sel <= checkOut && b.status != BookingStatus.CANCELLED
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GridCalendar(
                calendarMonth = calendarMonth,
                selectedDate = selectedDate,
                bookings = bookings,
                onDateSelected = { selectedDate = it },
                onMonthChange = { calendarMonth = it }
            )
        }

        item {
            val isTodaySelected = isSameDay(Calendar.getInstance(), Calendar.getInstance().apply { timeInMillis = selectedDate })
            Text(
                text = if (isTodaySelected) "Today's Bookings" else "Bookings for ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedDate))}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (bookingsForDate.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No bookings for this date", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            // Optimization: Use key for items
            items(bookingsForDate, key = { it.id }) { booking ->
                DashboardBookingCardRedesigned(
                    booking = booking, 
                    rooms = rooms, 
                    selectedDate = selectedDate,
                    onDelete = { onDeleteBooking(it) }
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun GridCalendar(
    calendarMonth: Calendar,
    selectedDate: Long,
    bookings: List<Booking>,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Calendar) -> Unit
) {
    val sdfMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newMonth = calendarMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, -1)
                    onMonthChange(newMonth)
                }) {
                    Icon(Icons.Default.ChevronLeft, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
                
                Text(
                    text = sdfMonth.format(calendarMonth.time),
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                IconButton(onClick = {
                    val newMonth = calendarMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, 1)
                    onMonthChange(newMonth)
                }) {
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
            }

            Column {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                val monthDates = remember(calendarMonth) { getDatesForMonth(calendarMonth) }
                val today = Calendar.getInstance()

                Column {
                    monthDates.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                if (date == null) {
                                    Spacer(Modifier.weight(1f))
                                } else {
                                    val cal = Calendar.getInstance().apply { time = date }
                                    val isToday = isSameDay(cal, today)
                                    val isSelected = isSameDay(cal, Calendar.getInstance().apply { timeInMillis = selectedDate })
                                    
                                    val hasCheckIn = bookings.any { b -> b.status != BookingStatus.CANCELLED && isSameDay(Calendar.getInstance().apply { timeInMillis = b.checkInDate }, cal) }
                                    val hasCheckOut = bookings.any { b -> b.status != BookingStatus.CANCELLED && isSameDay(Calendar.getInstance().apply { timeInMillis = b.checkOutDate }, cal) }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Color(0xFF1976D2)
                                                    isToday -> Color.White.copy(alpha = 0.5f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { onDateSelected(cal.timeInMillis) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color.Black
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (hasCheckIn) Box(Modifier.size(6.dp).background(Color(0xFF00C853), CircleShape))
                                                if (hasCheckOut) Box(Modifier.size(6.dp).background(Color(0xFF424242), CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                            if (week.size < 7) repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardBookingCardRedesigned(
    booking: Booking,
    rooms: List<Room>,
    selectedDate: Long,
    onDelete: (String) -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
    
    val isCheckIn = isSameDay(Calendar.getInstance().apply { timeInMillis = booking.checkInDate }, Calendar.getInstance().apply { timeInMillis = selectedDate })
    val isCheckOut = isSameDay(Calendar.getInstance().apply { timeInMillis = booking.checkOutDate }, Calendar.getInstance().apply { timeInMillis = selectedDate })
    val isStayOver = !isCheckIn && !isCheckOut && selectedDate > booking.checkInDate && selectedDate < booking.checkOutDate

    val businessDetails by com.example.roomservice.data.BusinessDetailsRepository.details.collectAsState()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            // TOP ROW: Guest Name and Action Icons
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
                    
                    if (booking.bookingAgent != "Manual Booking") {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = booking.bookingAgent,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Action Icons Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { com.example.roomservice.util.ReceiptHelper.printBookingReceipt(context, booking, businessDetails, roomType) }, modifier = Modifier.size(36.dp)) { 
                        Icon(Icons.Default.Print, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) 
                    }
                    IconButton(onClick = { com.example.roomservice.util.ReceiptHelper.shareReceiptOnWhatsApp(context, booking, businessDetails, roomType) }, modifier = Modifier.size(36.dp)) { 
                        Icon(Icons.Default.Share, null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp)) 
                    }
                    IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(36.dp)) { 
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) 
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            
            // MIDDLE ROW: Phone and Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (booking.guestPhone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL).apply { 
                                    data = android.net.Uri.parse("tel:${booking.guestPhone}") 
                                })
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
            
            // BOTTOM ROW: Financials and Status
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
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Delete Booking?",
            message = "Are you sure you want to permanently delete ${booking.guestName}\u0027s booking?",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { 
                onDelete(booking.id)
                showDeleteConfirm = false
            }
        )
    }

    if (showEditDialog) {
        AddBookingDialog(
            rooms = rooms,
            initialBooking = booking,
            onDismiss = { showEditDialog = false },
            onConfirm = { updated ->
                val finalBooking = if (updated.bookingNumber.isNullOrBlank()) {
                    updated.copy(bookingNumber = (1000000000L..9999999999L).random().toString())
                } else updated
                
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("hotels")
                    .child(booking.hotelId).child("bookings").child(booking.id).setValue(finalBooking)
                showEditDialog = false
            },
            onDeleteClick = {
                showEditDialog = false
                showDeleteConfirm = true
            }
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(8.dp)
            ) { Text("DELETE", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

private fun getDatesForMonth(month: Calendar): List<Date?> {
    val dates = mutableListOf<Date?>()
    val cal = month.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    repeat(cal.get(Calendar.DAY_OF_WEEK) - 1) { dates.add(null) }
    repeat(cal.getActualMaximum(Calendar.DAY_OF_MONTH)) { dates.add(cal.time); cal.add(Calendar.DAY_OF_MONTH, 1) }
    return dates
}

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

private fun isSameDay(c1: Calendar, c2: Calendar) = c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
