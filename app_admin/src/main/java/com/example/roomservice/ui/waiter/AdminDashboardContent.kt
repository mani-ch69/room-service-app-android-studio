package com.example.roomservice.ui.waiter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomservice.data.model.*
import com.example.roomservice.ui.common.BookingCard
import com.example.roomservice.ui.common.StatCard
import com.example.roomservice.ui.common.isSameDay
import com.example.roomservice.ui.util.AuroraBackground
import com.example.roomservice.ui.util.GlassCard
import com.example.roomservice.ui.util.GlassTextStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardContent(
    roomStatuses: List<RoomLiveStatus>,
    bookings: List<Booking> = emptyList(),
    onDeleteBooking: (String) -> Unit = {},
    viewModel: AdminMenuViewModel = viewModel()
) {
    val rooms = remember(roomStatuses) { roomStatuses.map { it.room } }
    val stats by viewModel.dashboardStats.collectAsState()
    val context = LocalContext.current
    
    val pullToRefreshState = rememberPullToRefreshState()

    // Auto-update status logic
    LaunchedEffect(bookings) {
        val now = Calendar.getInstance()
        bookings.forEach { booking ->
            if (booking.status == BookingStatus.BOOKED || booking.status == BookingStatus.CHECKED_IN) {
                val checkOutCal = Calendar.getInstance().apply { 
                    timeInMillis = booking.checkOutDate
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                }
                if (now.after(checkOutCal)) {
                    viewModel.updateBookingStatus(booking.id, BookingStatus.COMPLETED, context)
                }
            }
        }
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            delay(1500)
            pullToRefreshState.endRefresh()
        }
    }

    AuroraBackground {
        Box(modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            DashboardCalendarView(
                rooms = rooms, 
                bookings = bookings, 
                stats = stats,
                onDeleteBooking = onDeleteBooking,
                onStatusUpdate = { id, status -> viewModel.updateBookingStatus(id, status, context) }
            )

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color.White.copy(alpha = 0.1f),
                contentColor = Color.White
            )
        }
    }
}

@Composable
fun DashboardStatsRow(stats: DashboardStats) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Total", stats.totalBookings.toString(), Icons.Default.Inventory, Color(0xFF1976D2), Modifier.weight(1f))
        StatCard("Active", stats.activeStays.toString(), Icons.Default.CheckCircle, Color(0xFF2E7D32), Modifier.weight(1f))
        StatCard("Pending", stats.pendingArrivalsToday.toString(), Icons.Default.Pending, Color(0xFFF57C00), Modifier.weight(1f))
    }
}

@Composable
fun DashboardCalendarView(
    rooms: List<Room>,
    bookings: List<Booking>,
    stats: DashboardStats,
    onDeleteBooking: (String) -> Unit,
    onStatusUpdate: (String, BookingStatus) -> Unit
) {
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }

    val bookingsForDate by remember(bookings, selectedDate) {
        derivedStateOf {
            bookings.filter { b ->
                val checkIn = Calendar.getInstance().apply { timeInMillis = b.checkInDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                val checkOut = Calendar.getInstance().apply { timeInMillis = b.checkOutDate; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
                val sel = Calendar.getInstance().apply { timeInMillis = selectedDate; set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0) }.timeInMillis
                sel in checkIn..checkOut
            }
        }
    }

    val businessDetails by com.example.roomservice.data.BusinessDetailsRepository.details.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { DashboardStatsRow(stats) }

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
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp),
                style = GlassTextStyle
            )
        }

        if (bookingsForDate.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No bookings for this date", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            items(bookingsForDate, key = { it.id }) { booking ->
                var showStatusDialog by remember { mutableStateOf(false) }
                var showDeleteConfirm by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                
                val roomType = rooms.find { it.roomNumber == booking.roomNumber }?.roomType ?: ""

                BookingCard(
                    booking = booking,
                    rooms = rooms,
                    selectedDate = selectedDate,
                    onEdit = { showEditDialog = true },
                    onStatusClick = { showStatusDialog = true },
                    onPrint = { com.example.roomservice.util.ReceiptHelper.printBookingReceipt(context, booking, businessDetails, roomType) },
                    onWhatsApp = { com.example.roomservice.util.ReceiptHelper.shareReceiptOnWhatsApp(context, booking, businessDetails, roomType) }
                )

                if (showStatusDialog) {
                    StatusSelectionDialog(
                        currentStatus = booking.status,
                        onDismiss = { showStatusDialog = false },
                        onStatusSelected = {
                            onStatusUpdate(booking.id, it)
                            showStatusDialog = false
                        }
                    )
                }

                if (showDeleteConfirm) {
                    DeleteConfirmDialog(
                        title = "Delete Booking?",
                        message = "Are you sure you want to permanently delete ${booking.guestName}\u0027s booking?",
                        onDismiss = { showDeleteConfirm = false },
                        onConfirm = { 
                            onDeleteBooking(booking.id)
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
                            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("hotels")
                                .child(booking.hotelId).child("bookings").child(booking.id).setValue(updated)
                            showEditDialog = false
                        },
                        onDeleteClick = {
                            showEditDialog = false
                            showDeleteConfirm = true
                        }
                    )
                }
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
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            
            Text(
                text = sdfMonth.format(calendarMonth.time),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = Color.White
            )

            IconButton(onClick = {
                val newMonth = calendarMonth.clone() as Calendar
                newMonth.add(Calendar.MONTH, 1)
                onMonthChange(newMonth)
            }) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val monthDates = remember(calendarMonth) { getDatesForMonth(calendarMonth) }
        val today = Calendar.getInstance()

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
                                        isToday -> Color.White.copy(alpha = 0.15f)
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
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    if (hasCheckIn) Box(Modifier.size(6.dp).background(Color(0xFF81C784), CircleShape))
                                    if (hasCheckOut) Box(Modifier.size(6.dp).background(Color(0xFFE57373), CircleShape))
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

@Composable
fun StatusSelectionDialog(
    currentStatus: BookingStatus,
    onDismiss: () -> Unit,
    onStatusSelected: (BookingStatus) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Update Status", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(24.dp))
                StatusOptionRow("OK", Icons.Default.CheckCircle, Color(0xFF2E7D32), currentStatus == BookingStatus.COMPLETED) { onStatusSelected(BookingStatus.COMPLETED) }
                Spacer(Modifier.height(12.dp))
                StatusOptionRow("CANCELLED", Icons.Default.Cancel, Color.Red, currentStatus == BookingStatus.CANCELLED) { onStatusSelected(BookingStatus.CANCELLED) }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE", color = Color.Gray) }
            }
        }
    }
}

@Composable
fun StatusOptionRow(label: String, icon: ImageVector, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.1f) else Color(0xFFF8FAFC),
        border = if (isSelected) BorderStroke(2.dp, color) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(text = label, fontWeight = FontWeight.Bold, color = if (isSelected) color else Color.Black, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            if (isSelected) Icon(Icons.Default.Check, null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun DeleteConfirmDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(8.dp)) { Text("DELETE") }
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
