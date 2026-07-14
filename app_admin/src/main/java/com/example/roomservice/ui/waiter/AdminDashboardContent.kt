package com.example.roomservice.ui.waiter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.roomservice.ui.common.WhatsAppTemplateDialog
import com.example.roomservice.ui.util.AuroraBackground
import com.example.roomservice.ui.util.GlassCard
import com.example.roomservice.ui.util.GlassTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    
    var editingBooking by remember { mutableStateOf<Booking?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()

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
                onStatusUpdate = { id, status -> viewModel.updateBookingStatus(id, status, context) },
                onAddPayment = { editingBooking = it }
            )

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
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

@Composable
fun LiveStatsSummary(bookings: List<Booking>, selectedDate: Long) {
    val counts = remember(bookings, selectedDate) {
        val checkIns = bookings.count { b -> b.status != BookingStatus.CANCELLED && isSameDay(Calendar.getInstance().apply { timeInMillis = b.checkInDate }, Calendar.getInstance().apply { timeInMillis = selectedDate }) }
        val checkOuts = bookings.count { b -> b.status != BookingStatus.CANCELLED && isSameDay(Calendar.getInstance().apply { timeInMillis = b.checkOutDate }, Calendar.getInstance().apply { timeInMillis = selectedDate }) }
        val stayOvers = bookings.count { b -> 
            b.status != BookingStatus.CANCELLED && 
            !isSameDay(Calendar.getInstance().apply { timeInMillis = b.checkInDate }, Calendar.getInstance().apply { timeInMillis = selectedDate }) && 
            !isSameDay(Calendar.getInstance().apply { timeInMillis = b.checkOutDate }, Calendar.getInstance().apply { timeInMillis = selectedDate }) &&
            selectedDate > b.checkInDate && selectedDate < b.checkOutDate 
        }
        Triple(checkIns, stayOvers, checkOuts)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBadgeMini("Check-ins", counts.first, Color(0xFF1976D2), Modifier.weight(1f))
        StatBadgeMini("Stay-overs", counts.second, Color(0xFF1976D2), Modifier.weight(1f))
        StatBadgeMini("Check-outs", counts.third, Color(0xFF64748B), Modifier.weight(1f))
    }
}

@Composable
fun StatBadgeMini(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.7f))
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
    onStatusUpdate: (String, BookingStatus) -> Unit,
    onAddPayment: (Booking) -> Unit
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
            LiveStatsSummary(bookings, selectedDate)
        }

        item {
            // Space between stats and list
            Spacer(Modifier.height(4.dp))
        }

        if (bookingsForDate.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No bookings for this date", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            items(bookingsForDate, key = { it.id }) { booking ->
                var showStatusDialog by remember { mutableStateOf(false) }
                var showDeleteConfirm by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var showWhatsAppDialog by remember { mutableStateOf(false) }
                
                val roomType = rooms.find { it.roomNumber == booking.roomNumber }?.roomType ?: ""

                BookingCard(
                    booking = booking,
                    rooms = rooms,
                    selectedDate = selectedDate,
                    onEdit = { showEditDialog = true },
                    onStatusClick = { showStatusDialog = true },
                    onPrint = { com.example.roomservice.util.ReceiptHelper.printBookingReceipt(context, booking, businessDetails, roomType) },
                    onWhatsApp = { showWhatsAppDialog = true },
                    onWhatsAppReceipt = { com.example.roomservice.util.ReceiptHelper.shareReceiptOnWhatsApp(context, booking, businessDetails, roomType) },
                    onWhatsAppContact = { com.example.roomservice.util.ReceiptHelper.sendWhatsAppMessage(context, booking.guestPhone, "Hello ${booking.guestName}, this is Ganga Homestays.") },
                    actionButton = {
                        Button(
                            onClick = { onAddPayment(booking) },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.AddCard, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Payment", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GridCalendar(
    calendarMonth: Calendar,
    selectedDate: Long,
    bookings: List<Booking>,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Calendar) -> Unit
) {
    val sdfMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    
    // We use a large initial page to allow swiping left/right
    val initialPage = 500
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = initialPage)
    val coroutineScope = rememberCoroutineScope()

    // Sync month changes from pager swipes
    LaunchedEffect(pagerState.currentPage) {
        val diff = pagerState.currentPage - initialPage
        val targetMonth = Calendar.getInstance().apply {
            // Start from the current actual month
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, diff)
        }
        
        // Only update if it's actually a different month/year
        if (targetMonth.get(Calendar.MONTH) != calendarMonth.get(Calendar.MONTH) ||
            targetMonth.get(Calendar.YEAR) != calendarMonth.get(Calendar.YEAR)) {
            onMonthChange(targetMonth)
        }
    }

    // Handle button clicks to scroll the pager
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }) {
                Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
            }
            
            Text(
                text = sdfMonth.format(calendarMonth.time),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }) {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageMonth = remember(page) {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, page - initialPage)
                }
            }
            
            CalendarMonthPage(
                pageMonth = pageMonth,
                selectedDate = selectedDate,
                bookings = bookings,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
fun CalendarMonthPage(
    pageMonth: Calendar,
    selectedDate: Long,
    bookings: List<Booking>,
    onDateSelected: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val monthDates = remember(pageMonth) { getDatesForMonth(pageMonth) }
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
                                        isSelected -> MaterialTheme.colorScheme.primary // Blue
                                        isToday -> Color(0xFFF57C00) // Orange
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
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    if (hasCheckIn) Box(Modifier.size(6.dp).background(Color(0xFF1976D2), CircleShape))
                                    if (hasCheckOut) Box(Modifier.size(6.dp).background(Color(0xFF64748B), CircleShape))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Update Status", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(24.dp))
                StatusOptionRow("OK", Icons.Default.CheckCircle, Color(0xFF2E7D32), currentStatus == BookingStatus.COMPLETED) { onStatusSelected(BookingStatus.COMPLETED) }
                Spacer(Modifier.height(12.dp))
                StatusOptionRow("CANCELLED", Icons.Default.Cancel, Color(0xFFD32F2F), currentStatus == BookingStatus.CANCELLED) { onStatusSelected(BookingStatus.CANCELLED) }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CLOSE", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        color = if (isSelected) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isSelected) BorderStroke(2.dp, color) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(text = label, fontWeight = FontWeight.Bold, color = if (isSelected) color else MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
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
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(8.dp)) { Text("DELETE") }
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
