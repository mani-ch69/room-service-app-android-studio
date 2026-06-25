package com.example.roomservice.ui.waiter

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.roomservice.data.model.*
import com.example.roomservice.ui.waiter.RoomLiveStatus
import java.text.SimpleDateFormat
import java.util.*
import java.io.File

data class UnifiedRequest(
    val timestamp: Long,
    val order: Order? = null,
    val call: CallRequest? = null
)

@Composable
fun AdminDashboardContent(
    stats: DashboardStats,
    roomStatuses: List<RoomLiveStatus>,
    onAttendCall: (String) -> Unit,
    onChatClick: (String) -> Unit,
    staffList: List<Staff> = emptyList(),
    allOrders: List<Order> = emptyList(),
    allCalls: List<CallRequest> = emptyList(),
    bookings: List<Booking> = emptyList(),
    onStatusUpdate: (String, OrderStatus) -> Unit = { _, _ -> },
    onAssignStaffToCall: (String, Staff) -> Unit = { _, _ -> },
    onAssignStaffToOrder: (String, Staff) -> Unit = { _, _ -> },
    onDeleteBooking: (String) -> Unit = {},
    onAddBooking: (Booking) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val unifiedFeed = remember(allOrders, allCalls) {
        val orders = allOrders.map { UnifiedRequest(it.timestamp, order = it) }
        val calls = allCalls.map { UnifiedRequest(it.timestamp, call = it) }
        
        (orders + calls).sortedByDescending { it.timestamp }
            .groupBy { 
                val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.timestamp))
                val cal = Calendar.getInstance()
                val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)
                
                when (dateStr) {
                    todayStr -> "Today"
                    yesterdayStr -> "Yesterday"
                    else -> SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(it.timestamp))
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            },
            divider = { HorizontalDivider(thickness = 0.5.dp, color = Color.Transparent) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Bookings", fontSize = 14.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assignment, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Orders", fontSize = 14.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
        }

        if (selectedTab == 0) {
            // BOOKINGS TAB
            val rooms = remember(roomStatuses) { roomStatuses.map { it.room } }
            DashboardCalendarView(
                rooms = rooms, 
                bookings = bookings, 
                onAddBooking = onAddBooking,
                onUpdateStatus = onStatusUpdate
            )
        } else {
            if (unifiedFeed.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Default.Inbox, "No active orders")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    unifiedFeed.forEach { (dateLabel, requests) ->
                        item {
                            Text(
                                text = dateLabel,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(requests) { req ->
                            if (req.order != null) {
                                OrderItemCard(
                                    order = req.order,
                                    staffList = staffList,
                                    onStatusUpdate = onStatusUpdate,
                                    onAssignStaff = onAssignStaffToOrder
                                )
                            } else if (req.call != null) {
                                ActiveRequestCard(
                                    status = RoomLiveStatus(com.example.roomservice.data.model.Room(roomNumber = req.call.roomNumber), activeCall = req.call),
                                    onAttendCall = onAttendCall,
                                    onChatClick = onChatClick,
                                    staffList = staffList,
                                    onAssignStaffToCall = onAssignStaffToCall,
                                    onAssignStaffToOrder = onAssignStaffToOrder
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun DashboardCalendarView(
    rooms: List<Room>,
    bookings: List<Booking>,
    onAddBooking: (Booking) -> Unit,
    onUpdateStatus: (String, OrderStatus) -> Unit
) {
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val bookingsForDate = remember(bookings, selectedDate) {
        bookings.filter { b ->
            val checkIn = Calendar.getInstance().apply { timeInMillis = b.checkInDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val checkOut = Calendar.getInstance().apply { timeInMillis = b.checkOutDate; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
            selectedDate >= checkIn && selectedDate <= checkOut && b.status != BookingStatus.CANCELLED
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Calendar Item
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    // Header: Month & Arrows Centered
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val newMonth = calendarMonth.clone() as Calendar
                            newMonth.add(Calendar.MONTH, -1)
                            calendarMonth = newMonth
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
                            calendarMonth = newMonth
                        }) {
                            Icon(Icons.Default.ChevronRight, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                        }
                    }

                    AnimatedVisibility(visible = true) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            // Days Header
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

                            // Calendar Grid
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
                                                        .clickable { selectedDate = cal.timeInMillis },
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
                                        // FILL REMAINING SLOTS IN LAST WEEK
                                        if (week.size < 7) {
                                            repeat(7 - week.size) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Bookings List for Selected Date
        if (bookingsForDate.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No bookings for this date", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            items(bookingsForDate) { booking ->
                DashboardBookingCardRedesigned(
                    booking = booking, 
                    rooms = rooms, 
                    selectedDate = selectedDate,
                    onUpdateStatus = onUpdateStatus,
                    onClick = { /* Detail if needed */ }
                )
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

private fun getDatesForMonth(month: Calendar): List<Date?> {
    val dates = mutableListOf<Date?>()
    val cal = month.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    repeat(firstDayOfWeek) { dates.add(null) }
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    repeat(maxDay) {
        dates.add(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return dates
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun DashboardBookingCardRedesigned(
    booking: Booking,
    rooms: List<Room>,
    selectedDate: Long,
    onUpdateStatus: (String, OrderStatus) -> Unit = { _, _ -> },
    onClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val df = SimpleDateFormat("dd MMM", Locale.getDefault())
    val checkInStr = df.format(Date(booking.checkInDate))
    val checkOutStr = df.format(Date(booking.checkOutDate))
    
    val roomType = remember(booking.roomNumber, rooms) {
        rooms.find { it.roomNumber == booking.roomNumber }?.roomType ?: ""
    }

    val outstanding = booking.totalAmount - booking.advancePaid

    val nights = remember(booking.checkInDate, booking.checkOutDate) {
        val start = Calendar.getInstance().apply { 
            timeInMillis = booking.checkInDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply { 
            timeInMillis = booking.checkOutDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        ((end.timeInMillis - start.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
    }
    
    val isCheckIn = isSameDay(
        Calendar.getInstance().apply { timeInMillis = booking.checkInDate },
        Calendar.getInstance().apply { timeInMillis = selectedDate }
    )
    val isCheckOut = isSameDay(
        Calendar.getInstance().apply { timeInMillis = booking.checkOutDate },
        Calendar.getInstance().apply { timeInMillis = selectedDate }
    )
    val isStayOver = !isCheckIn && !isCheckOut && selectedDate > booking.checkInDate && selectedDate < booking.checkOutDate

    var guestIdentities by remember(booking.id, booking.guestIdentities) { 
        mutableStateOf(if (booking.guestIdentities.isNotEmpty()) booking.guestIdentities 
                      else List(booking.numberOfGuests) { GuestIdentity() }) 
    }

    fun updateGuestIdInfo(index: Int, type: String? = null, number: String? = null) {
        val newList = guestIdentities.toMutableList()
        val current = newList[index]
        newList[index] = current.copy(
            idType = type ?: current.idType,
            idNumber = number ?: current.idNumber
        )
        guestIdentities = newList
    }

    fun updateGuestPhoto(index: Int, isFront: Boolean, uri: android.net.Uri) {
        val newList = guestIdentities.toMutableList()
        val current = newList[index]
        newList[index] = if (isFront) current.copy(frontPhotoUrl = uri.toString()) 
                         else current.copy(backPhotoUrl = uri.toString())
        guestIdentities = newList
    }

    val businessDetails by com.example.roomservice.data.BusinessDetailsRepository.details.collectAsState()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = booking.guestName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
                    
                    if (booking.guestPhone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = booking.guestPhone, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                data = android.net.Uri.parse("tel:${booking.guestPhone}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    Text(text = "$checkInStr - $checkOutStr", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                    Text(text = "$nights nights • ${booking.numberOfGuests} guests", fontSize = 13.sp, color = Color.Gray)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text("Total", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("₹${booking.totalAmount}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Paid", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("₹${booking.advancePaid}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("Pending", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("₹$outstanding", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (isCheckIn) {
                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                                Text(text = "Check-in", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else if (isCheckOut) {
                            Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                                Text(text = "Check-out", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else if (isStayOver) {
                            Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp)) {
                                Text(text = "Stay over", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(text = "Room ${booking.roomNumber}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        if (roomType.isNotEmpty()) {
                            Text(text = roomType, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            com.example.roomservice.util.ReceiptHelper.printBookingReceipt(context, booking, businessDetails, roomType)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Print, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (booking.status != BookingStatus.COMPLETED) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    
                    if (booking.status == BookingStatus.BOOKED) {
                        Button(
                            onClick = {
                                com.example.roomservice.data.BookingRepository.updateBookingStatus(booking.id, BookingStatus.CHECKED_IN)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("COMPLETE CHECK-IN", fontWeight = FontWeight.Bold)
                        }
                    } else if (booking.status == BookingStatus.CHECKED_IN) {
                        Button(
                            onClick = { com.example.roomservice.data.BookingRepository.updateBookingStatus(booking.id, BookingStatus.COMPLETED) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("CHECK OUT GUEST", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showEditDialog) {
        AddBookingDialog(
            rooms = rooms,
            initialBooking = booking,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedBooking ->
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("hotels")
                    .child(booking.hotelId).child("bookings").child(booking.id)
                    .setValue(updatedBooking)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookingDialog(
    rooms: List<Room>,
    initialBooking: Booking? = null,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit
) {
    var guestName by remember { mutableStateOf(initialBooking?.guestName ?: "") }
    var guestPhone by remember { mutableStateOf(initialBooking?.guestPhone ?: "") }
    
    val roomTypes = remember(rooms) { 
        listOf("All") + rooms.map { it.roomType }.distinct().sorted()
    }
    var selectedType by remember { mutableStateOf("All") }
    var typeExp by remember { mutableStateOf(false) }
    
    var selectedRoom by remember { mutableStateOf(initialBooking?.roomNumber ?: "") }
    var roomExp by remember { mutableStateOf(false) }

    var totalAmount by remember { mutableStateOf(initialBooking?.totalAmount?.toString() ?: "") }
    var advancePaid by remember { mutableStateOf(initialBooking?.advancePaid?.toString() ?: "") }
    var numberOfGuests by remember { mutableIntStateOf(initialBooking?.numberOfGuests ?: 1) }

    val roomsOfType = remember(selectedType, rooms) { 
        if (selectedType == "All") rooms 
        else rooms.filter { it.roomType.contains(selectedType, ignoreCase = true) } 
    }
    
    // Auto-select first room when list or type changes (only for new bookings)
    LaunchedEffect(roomsOfType) {
        if (initialBooking == null && roomsOfType.isNotEmpty()) {
            if (selectedRoom.isEmpty() || !roomsOfType.any { it.roomNumber == selectedRoom }) {
                selectedRoom = roomsOfType.first().roomNumber
            }
        }
    }

    var showRangePicker by remember { mutableStateOf(false) }
    var checkInDate by remember { mutableLongStateOf(initialBooking?.checkInDate ?: System.currentTimeMillis()) }
    var checkOutDate by remember { mutableLongStateOf(initialBooking?.checkOutDate ?: (System.currentTimeMillis() + (24 * 60 * 60 * 1000L))) }

    val hotelId by com.example.roomservice.data.HotelSession.hotelId.collectAsState()
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    if (showRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = checkInDate,
            initialSelectedEndDateMillis = checkOutDate
        )
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateRangePickerState.selectedStartDateMillis?.let { checkInDate = it }
                    dateRangePickerState.selectedEndDateMillis?.let { checkOutDate = it }
                    showRangePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text("CANCEL") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f).padding(16.dp),
                title = { Text("Select Booking Dates", modifier = Modifier.padding(16.dp)) }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBooking == null) "New Booking" else "Edit Booking", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(value = guestName, onValueChange = { guestName = it }, label = { Text("Guest Name") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = guestPhone, onValueChange = { guestPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Room Type Selection
                        ExposedDropdownMenuBox(
                            expanded = typeExp,
                            onExpandedChange = { typeExp = !typeExp },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Room Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExp) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = typeExp, onDismissRequest = { typeExp = false }) {
                                roomTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = { selectedType = type; typeExp = false }
                                    )
                                }
                            }
                        }

                        // Room Number Selection (Filtered by Type)
                        ExposedDropdownMenuBox(
                            expanded = roomExp,
                            onExpandedChange = { roomExp = !roomExp },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (selectedRoom.isEmpty()) "No Rooms" else "Room $selectedRoom",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Room No.") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roomExp) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = roomExp, onDismissRequest = { roomExp = false }) {
                                if (roomsOfType.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No rooms available") }, onClick = { roomExp = false })
                                } else {
                                    roomsOfType.forEach { room ->
                                        DropdownMenuItem(
                                            text = { Text("Room ${room.roomNumber}") },
                                            onClick = { selectedRoom = room.roomNumber; roomExp = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Surface(
                        onClick = { showRangePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.LightGray),
                        color = Color.White
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Check-In / Check-Out", fontSize = 10.sp, color = Color.Gray)
                                Text("${df.format(Date(checkInDate))} - ${df.format(Date(checkOutDate))}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            }
                            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Guests:", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (numberOfGuests > 1) numberOfGuests-- }) {
                                Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "$numberOfGuests",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { numberOfGuests++ }) {
                                Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = totalAmount, onValueChange = { totalAmount = it }, label = { Text("Total Bill") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                        OutlinedTextField(value = advancePaid, onValueChange = { advancePaid = it }, label = { Text("Advance") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bId = initialBooking?.bookingNumber ?: ("BK-" + System.currentTimeMillis().toString().takeLast(6))
                    onConfirm(Booking(
                        id = initialBooking?.id ?: UUID.randomUUID().toString(),
                        bookingNumber = bId,
                        hotelId = hotelId ?: "",
                        roomNumber = selectedRoom,
                        guestName = guestName,
                        guestPhone = guestPhone,
                        checkInDate = checkInDate,
                        checkOutDate = checkOutDate,
                        totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                        advancePaid = advancePaid.toDoubleOrNull() ?: 0.0,
                        numberOfGuests = numberOfGuests,
                        guestIdentities = initialBooking?.guestIdentities ?: emptyList(),
                        status = initialBooking?.status ?: BookingStatus.BOOKED,
                        timestamp = initialBooking?.timestamp ?: System.currentTimeMillis()
                    ))
                },
                enabled = guestName.isNotBlank() && selectedRoom.isNotBlank() && checkOutDate > checkInDate
            ) {
                Text(if (initialBooking == null) "SAVE BOOKING" else "UPDATE BOOKING")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun ActiveRequestCard(
    status: RoomLiveStatus,
    onAttendCall: (String) -> Unit,
    onChatClick: (String) -> Unit,
    staffList: List<Staff> = emptyList(),
    onAssignStaffToCall: (String, Staff) -> Unit = { _, _ -> },
    onAssignStaffToOrder: (String, Staff) -> Unit = { _, _ -> }
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAssignmentDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isCall = status.activeCall != null
                val tint = if (isCall) Color.Red else Color(0xFF2E7D32)
                
                Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = tint.copy(alpha = 0.1f)) {
                    Icon(if (isCall) Icons.Default.NotificationsActive else Icons.Default.Fastfood, null, modifier = Modifier.padding(8.dp), tint = tint)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Room ${status.room.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                    Text(if (isCall) "Service Call" else "Food Order Request", color = tint, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
                
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }

                if (isCall) {
                    Button(onClick = { showAssignmentDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = tint), contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(32.dp)) {
                        Text("Assign", fontSize = 12.sp)
                    }
                } else {
                    IconButton(onClick = { onChatClick(status.room.roomNumber) }) { Icon(Icons.Default.Chat, null, tint = Color.Gray) }
                }
            }
            
            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                Text("Active request requires attention. Assign staff or respond via chat.", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }

    if (showAssignmentDialog) {
        val filteredStaff = if (status.activeCall != null) staffList.filter { it.role == "Housekeeping" } else staffList
        StaffAssignmentDialog(staffList = filteredStaff, onDismiss = { showAssignmentDialog = false }, onAssign = { staff ->
            if (status.activeCall != null) onAssignStaffToCall(status.activeCall.id, staff)
            showAssignmentDialog = false
        })
    }
}


@Composable
fun StaffAssignmentDialog(staffList: List<Staff>, onDismiss: () -> Unit, onAssign: (Staff) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Staff", fontWeight = FontWeight.Bold) },
        text = {
            if (staffList.isEmpty()) {
                Text("No staff available for this role.", color = Color.Gray)
            } else {
                LazyColumn {
                    items(staffList) { staff ->
                        ListItem(
                            headlineContent = { Text(staff.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(staff.role) },
                            leadingContent = {
                                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(staff.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onAssign(staff) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun EmptyState(icon: ImageVector, message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text(message, color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OrdersList(orders: List<Order>, staffList: List<Staff>, onStatusUpdate: (String, OrderStatus) -> Unit, onAssignStaff: (String, Staff) -> Unit = { _, _ -> }) {
    if (orders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(Icons.Default.Inbox, "No orders found")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(orders.sortedByDescending { it.timestamp }) { order ->
                OrderItemCard(
                    order = order,
                    staffList = staffList,
                    onStatusUpdate = onStatusUpdate,
                    onAssignStaff = onAssignStaff
                )
            }
        }
    }
}

@Composable
fun OrderItemCard(order: Order, staffList: List<Staff>, onStatusUpdate: (String, OrderStatus) -> Unit, onAssignStaff: (String, Staff) -> Unit = { _, _ -> }) {
    var showAssignmentDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Order #${order.id.takeLast(6)}", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    Text("Room ${order.roomNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
                }
                Surface(
                    color = getStatusColor(order.status).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.status.name,
                        color = getStatusColor(order.status),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            order.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.quantity}x ${item.name}", fontSize = 14.sp, color = Color.DarkGray)
                    Text("₹${item.price * item.quantity}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Total Amount", fontSize = 11.sp, color = Color.Gray)
                    Text("₹${order.totalAmount}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
                
                if (order.status == OrderStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onStatusUpdate(order.id, OrderStatus.CANCELLED) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Decline", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onStatusUpdate(order.id, OrderStatus.ACCEPTED) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Accept", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (order.status == OrderStatus.ACCEPTED || order.status == OrderStatus.PROCESSING) {
                    if (order.assignedStaffId == null) {
                        Button(
                            onClick = { showAssignmentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Assign Staff")
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Assigned to:", fontSize = 10.sp, color = Color.Gray)
                            Text(order.assignedStaffName ?: "", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            TextButton(onClick = { onStatusUpdate(order.id, OrderStatus.DELIVERED) }) {
                                Text("Mark Delivered", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAssignmentDialog) {
        val filteredStaff = staffList.filter { it.role == "Waiter" }
        StaffAssignmentDialog(staffList = filteredStaff, onDismiss = { showAssignmentDialog = false }, onAssign = { staff ->
            onAssignStaff(order.id, staff)
            showAssignmentDialog = false
        })
    }
}

private fun getStatusColor(status: OrderStatus): Color {
    return when (status) {
        OrderStatus.PENDING -> Color(0xFFE65100)
        OrderStatus.ACCEPTED -> Color(0xFF1976D2)
        OrderStatus.PROCESSING -> Color(0xFFFFA000)
        OrderStatus.DELIVERED -> Color(0xFF2E7D32)
        OrderStatus.CANCELLED, OrderStatus.NOT_ACCEPTED -> Color.Red
        else -> Color.Gray
    }
}

@Composable
fun IdentityCaptureBox(
    label: String,
    currentUrl: String?,
    onCapture: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOptions by remember { mutableStateOf(false) }

    val cameraImageUri = remember { 
        try { 
            val file = File(context.cacheDir, "id_temp_${System.currentTimeMillis()}.jpg")
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null } 
    }
    
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) onCapture(cameraImageUri)
    }
    
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onCapture(uri)
    }
    
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted && cameraImageUri != null) cameraLauncher.launch(cameraImageUri)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .background(Color.LightGray.copy(alpha = 0.1f))
                .clickable { showOptions = true },
            contentAlignment = Alignment.Center
        ) {
            if (currentUrl != null) {
                AsyncImage(
                    model = currentUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("Select ID $label") },
            text = { Text("Choose a method to provide the identity document.") },
            confirmButton = {
                TextButton(onClick = { 
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        cameraImageUri?.let { cameraLauncher.launch(it) }
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                    showOptions = false 
                }) { Text("CAMERA") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    galleryLauncher.launch("image/*")
                    showOptions = false 
                }) { Text("GALLERY") }
            }
        )
    }
}
