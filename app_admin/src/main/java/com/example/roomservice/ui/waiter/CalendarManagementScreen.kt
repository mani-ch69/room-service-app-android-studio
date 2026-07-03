package com.example.roomservice.ui.waiter

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BookingStatus
import com.example.roomservice.data.model.Room
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarManagementScreen(
    rooms: List<Room>,
    bookings: List<Booking>
) {
    var selectedTab by remember { mutableStateOf("List") }
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val sdfMonth = remember { SimpleDateFormat("MMMM", Locale.getDefault()) }
    
    var showFullCalendarForType by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // Tab Header
        TabRow(
            selectedTabIndex = if (selectedTab == "List") 0 else 1,
            containerColor = Color.White,
            contentColor = Color(0xFF1976D2),
            divider = {}
        ) {
            Tab(selected = selectedTab == "List", onClick = { selectedTab = "List" }) {
                Text("List", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedTab == "Calendar", onClick = { selectedTab = "Calendar" }) {
                Text("Calendar", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
        }

        // Month Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                calendarMonth = (calendarMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            }) { Icon(Icons.Default.ChevronLeft, null, tint = Color(0xFF1976D2)) }
            
            Text(
                text = sdfMonth.format(calendarMonth.time),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            IconButton(onClick = {
                calendarMonth = (calendarMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            }) { Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF1976D2)) }
        }

        if (selectedTab == "List") {
            CalendarListContent(calendarMonth, rooms, bookings)
        } else {
            CalendarGridContent(calendarMonth, rooms, bookings) { showFullCalendarForType = it }
        }
    }
    
    if (showFullCalendarForType != null) {
        FullCalendarViewDialog(
            roomType = showFullCalendarForType!!,
            rooms = rooms,
            bookings = bookings,
            initialMonth = calendarMonth,
            onDismiss = { showFullCalendarForType = null }
        )
    }
}

@Composable
fun CalendarListContent(month: Calendar, rooms: List<Room>, bookings: List<Booking>) {
    val sdfDate = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val roomTypes = rooms.map { it.roomType }.distinct().sorted()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items((1..daysInMonth).toList()) { day ->
            val dateCal = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
            
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(
                    text = sdfDate.format(dateCal.time),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column {
                        roomTypes.forEach { type ->
                            val roomsOfType = rooms.filter { it.roomType == type }
                            val bookedCount = bookings.count { b ->
                                b.status != BookingStatus.CANCELLED &&
                                roomsOfType.any { it.roomNumber == b.roomNumber } &&
                                dateCal.timeInMillis >= startOfDay(b.checkInDate) &&
                                dateCal.timeInMillis < startOfDay(b.checkOutDate)
                            }
                            val availableCount = (roomsOfType.size - bookedCount).coerceAtLeast(0)
                            val isClosed = !roomsOfType.any { it.isAvailable } // Simplified for now

                            RoomTypeListItem(type, isClosed, availableCount, bookedCount)
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomTypeListItem(type: String, isClosed: Boolean, available: Int, booked: Int) {
    var closedState by remember { mutableStateOf(isClosed) }
    var availableState by remember { mutableIntStateOf(available) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = type, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(
                    color = if(closedState) Color(0xFFC62828).copy(alpha = 0.1f) else Color(0xFF2E7D32).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if(closedState) "Closed" else "Bookable",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(closedState) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }
            }
            Switch(checked = !closedState, onCheckedChange = { closedState = !it })
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Available", fontSize = 13.sp, color = Color.Gray)
                Text("Booked: $booked", fontSize = 11.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if(availableState > 0) availableState-- }) { Icon(Icons.Default.Remove, null, tint = Color(0xFF1976D2)) }
                Text("$availableState", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { availableState++ }) { Icon(Icons.Default.Add, null, tint = Color(0xFF1976D2)) }
            }
        }
        
        Text(
            "Rates & restrictions", 
            fontSize = 13.sp, 
            color = Color(0xFF1976D2), 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp).clickable { }
        )
    }
}

@Composable
fun CalendarGridContent(month: Calendar, rooms: List<Room>, bookings: List<Booking>, onTypeClick: (String) -> Unit) {
    val roomTypes = rooms.map { it.roomType }.distinct().sorted()
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            // FlowRow equivalent for mobile grid
            val chunks = roomTypes.chunked(2)
            chunks.forEach { rowTypes ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowTypes.forEach { type ->
                        Box(modifier = Modifier.weight(1f)) {
                            MiniCalendarCard(type, month, rooms.filter { it.roomType == type }, bookings) { onTypeClick(type) }
                        }
                    }
                    if (rowTypes.size < 2) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MiniCalendarCard(type: String, month: Calendar, roomsOfType: List<Room>, bookings: List<Booking>, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { 
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
            }
        }
        
        val dates = getDatesForMonth(month)
        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f))
                    else {
                        val cal = Calendar.getInstance().apply { time = date }
                        val bookedCount = bookings.count { b ->
                            b.status != BookingStatus.CANCELLED &&
                            roomsOfType.any { it.roomNumber == b.roomNumber } &&
                            cal.timeInMillis >= startOfDay(b.checkInDate) &&
                            cal.timeInMillis < startOfDay(b.checkOutDate)
                        }
                        
                        val isFullyBooked = bookedCount >= roomsOfType.size && roomsOfType.isNotEmpty()
                        val isClosed = !roomsOfType.any { it.isAvailable } // Simplified

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isClosed -> Color(0xFFFFEBEE) // Red for closed
                                        isFullyBooked -> Color(0xFFFFF3E0) // Yellow/Orange for occupied
                                        else -> Color(0xFFE8F5E9) // Green for open
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cal.get(Calendar.DAY_OF_MONTH).toString(), fontSize = 9.sp)
                        }
                    }
                }
                if (week.size < 7) repeat(7-week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text(type, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullCalendarViewDialog(
    roomType: String,
    rooms: List<Room>,
    bookings: List<Booking>,
    initialMonth: Calendar,
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(initialMonth) }
    val sdfMonth = remember { SimpleDateFormat("MMMM", Locale.getDefault()) }
    val roomsOfType = rooms.filter { it.roomType == roomType }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column {
                TopAppBar(
                    title = { Text(roomType, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    actions = { IconButton(onClick = { }) { Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF1976D2)) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                
                // Month Nav
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }) { Icon(Icons.Default.ChevronLeft, null) }
                    Text(sdfMonth.format(currentMonth.time), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }) { Icon(Icons.Default.ChevronRight, null) }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { 
                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray)
                    }
                }

                val dates = getDatesForMonth(currentMonth)
                Column(modifier = Modifier.padding(8.dp)) {
                    dates.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                if (date == null) Spacer(Modifier.weight(1f))
                                else {
                                    val cal = Calendar.getInstance().apply { time = date }
                                    val bookedCount = bookings.count { b ->
                                        b.status != BookingStatus.CANCELLED &&
                                        roomsOfType.any { it.roomNumber == b.roomNumber } &&
                                        cal.timeInMillis >= startOfDay(b.checkInDate) &&
                                        cal.timeInMillis < startOfDay(b.checkOutDate)
                                    }
                                    val isFullyBooked = bookedCount >= roomsOfType.size && roomsOfType.isNotEmpty()
                                    val isClosed = !roomsOfType.any { it.isAvailable }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .border(
                                                width = 1.dp,
                                                color = if (isFullyBooked) Color(0xFFF57C00) else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .background(
                                                when {
                                                    isClosed -> Color(0xFFE0E0E0)
                                                    else -> Color(0xFFF1FDF4)
                                                },
                                                RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if(isClosed) Color.Gray else Color.Black
                                        )
                                    }
                                }
                            }
                            if(week.size < 7) repeat(7-week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                
                // Legend
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    LegendBox(Color(0xFFF1FDF4), Color(0xFFE2E8F0), "Bookable")
                    LegendBox(Color(0xFFF1FDF4), Color(0xFFF57C00), "Sold out")
                    LegendBox(Color(0xFFE0E0E0), Color(0xFFE2E8F0), "Closed")
                }
                
                Spacer(Modifier.height(40.dp))
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.GridOn, null, modifier = Modifier.size(100.dp).alpha(0.1f), tint = Color(0xFF1976D2))
                    Spacer(Modifier.height(16.dp))
                    Text("Select dates to make changes", fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun LegendBox(bg: Color, border: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).background(bg, RoundedCornerShape(2.dp)).border(1.dp, border, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp)
    }
}

private fun getDatesForMonth(month: Calendar): List<Date?> {
    val dates = mutableListOf<Date?>()
    val cal = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    repeat(firstDayOfWeek) { dates.add(null) }
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    repeat(maxDay) {
        dates.add(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return dates
}

private fun startOfDay(ts: Long): Long = Calendar.getInstance().apply { 
    timeInMillis = ts; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) 
}.timeInMillis

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
