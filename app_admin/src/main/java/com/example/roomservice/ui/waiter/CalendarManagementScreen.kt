package com.example.roomservice.ui.waiter

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    var showFullCalendarForType by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        CalendarGridContent(calendarMonth, rooms, bookings) { showFullCalendarForType = it }
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
    val sdfMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val roomsOfType = rooms.filter { it.roomType == roomType }

    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var showBulkEditForType by remember { mutableStateOf<String?>(null) }
    var showRatePlanEdit by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column {
                TopAppBar(
                    title = { Text(roomType, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    actions = { 
                        IconButton(onClick = { showBulkEditForType = roomType }) { 
                            Icon(Icons.Default.Edit, contentDescription = "Bulk Edit", tint = Color(0xFF1976D2)) 
                        } 
                    },
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
                                    val isSelected = selectedDate?.let { isSameDay(cal, Calendar.getInstance().apply { time = it }) } ?: false
                                    
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
                                                width = if(isSelected) 2.dp else 1.dp,
                                                color = when {
                                                    isSelected -> Color(0xFF1976D2)
                                                    isFullyBooked -> Color(0xFFF57C00)
                                                    else -> Color(0xFFE2E8F0)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .background(
                                                when {
                                                    isClosed -> Color(0xFFE0E0E0)
                                                    else -> Color(0xFFF1FDF4)
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if(isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if(isClosed) Color.Gray else if(isSelected) Color(0xFF1976D2) else Color.Black
                                        )
                                    }
                                }
                            }
                            if(week.size < 7) repeat(7-week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                if (selectedDate == null) {
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
                } else {
                    // MANAGEMENT PANEL
                    val cal = Calendar.getInstance().apply { time = selectedDate!! }
                    val bookedCount = bookings.count { b ->
                        b.status != BookingStatus.CANCELLED &&
                        roomsOfType.any { it.roomNumber == b.roomNumber } &&
                        cal.timeInMillis >= startOfDay(b.checkInDate) &&
                        cal.timeInMillis < startOfDay(b.checkOutDate)
                    }
                    
                    DateManagementPanel(
                        date = selectedDate!!,
                        bookedCount = bookedCount,
                        totalCount = roomsOfType.size,
                        onDismiss = { selectedDate = null },
                        onSave = { selectedDate = null },
                        onEditRatePlan = { showRatePlanEdit = true }
                    )
                }
            }
        }
    }

    if (showRatePlanEdit && selectedDate != null) {
        RatePlanEditDialog(
            date = selectedDate!!,
            onDismiss = { showRatePlanEdit = false },
            onSave = { showRatePlanEdit = false }
        )
    }

    if (showBulkEditForType != null) {
        BulkEditDialog(
            roomType = showBulkEditForType!!,
            rooms = rooms.filter { it.roomType == showBulkEditForType },
            onDismiss = { showBulkEditForType = null }
        )
    }
}

@Composable
fun DateManagementPanel(
    date: Date,
    bookedCount: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onEditRatePlan: () -> Unit
) {
    var isOpen by remember { mutableStateOf(true) }
    var roomsToSell by remember { mutableIntStateOf(totalCount - bookedCount) }
    val sdfFull = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AVAILABILITY SECTION
        item {
            Column {
                Text("Availability", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isOpen = true }.padding(12.dp)) {
                            Text("Open", modifier = Modifier.weight(1f))
                            RadioButton(selected = isOpen, onClick = { isOpen = true })
                        }
                        Divider(color = Color(0xFFF1F5F9))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isOpen = false }.padding(12.dp)) {
                            Text("Close", modifier = Modifier.weight(1f))
                            RadioButton(selected = !isOpen, onClick = { isOpen = false })
                        }
                    }
                }
            }
        }

        // INVENTORY SECTION
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Booked/Total", color = Color.Gray)
                        Text("$bookedCount / $totalCount", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Rooms to sell", color = Color.Gray)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp)
                        ) {
                            IconButton(onClick = { if(roomsToSell > 0) roomsToSell-- }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
                            Text("$roomsToSell room", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { if(roomsToSell < totalCount) roomsToSell++ }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
            }
        }

        // RATE PLANS SECTION
        item {
            Column {
                Text("Rate plans", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Standard rate", fontWeight = FontWeight.Bold)
                            Text("Edit", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onEditRatePlan() })
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Text(" x2", fontSize = 14.sp, color = Color.Gray)
                            Spacer(Modifier.weight(1f))
                            Text("Rs.700", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Text("Minimum length of stay: 1 night", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        // BOTTOM ACTIONS
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sdfFull.format(date), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Selected", fontSize = 12.sp, color = Color.Gray)
                }
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.7f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear", color = Color(0xFF1976D2))
                }
                
                Button(
                    onClick = {
                        Toast.makeText(context, "Availability saved for ${sdfFull.format(date)}", Toast.LENGTH_SHORT).show()
                        onSave()
                    },
                    modifier = Modifier.weight(0.7f).height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatePlanEditDialog(
    date: Date,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var isActive by remember { mutableStateOf(true) }
    var priceX2 by remember { mutableStateOf("700") }
    var priceX3 by remember { mutableStateOf("840") }
    var priceX4 by remember { mutableStateOf("875") }
    var minStay by remember { mutableStateOf("1 night") }
    var minAdvance by remember { mutableStateOf("None") }
    
    val sdfFull = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Standard rate", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    actions = { 
                        TextButton(onClick = { 
                            isActive = true
                            priceX2 = "700"
                            priceX3 = "840"
                            priceX4 = "875"
                            minStay = "1 night"
                            minAdvance = "None"
                        }) { Text("Reset", color = Color.Gray) } 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Switch(checked = isActive, onCheckedChange = { isActive = it }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1976D2)))
                        }
                        Divider(modifier = Modifier.padding(top = 16.dp), color = Color(0xFFF1F5F9))
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OccupancyPriceRow("x2", "Base", priceX2, true) { priceX2 = it }
                            OccupancyPriceRow("x4", null, priceX4, isActive) { priceX4 = it }
                            OccupancyPriceRow("x3", null, priceX3, isActive) { priceX3 = it }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            RestrictionRow("Minimum length of stay", minStay) { 
                                // Simplified: Cycle through options
                                minStay = when(minStay) {
                                    "1 night" -> "2 nights"
                                    "2 nights" -> "3 nights"
                                    else -> "1 night"
                                }
                            }
                            RestrictionRow("Minimum advance reservation", minAdvance) { 
                                minAdvance = when(minAdvance) {
                                    "None" -> "1 day"
                                    "1 day" -> "2 days"
                                    else -> "None"
                                }
                            }
                        }
                    }
                }

                // BOTTOM ACTION BAR
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sdfFull.format(date), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Selected", fontSize = 12.sp, color = Color.Gray)
                        }
                        
                        Button(
                            onClick = {
                                Toast.makeText(context, "Rate plan updated", Toast.LENGTH_SHORT).show()
                                onSave()
                            },
                            modifier = Modifier.width(120.dp).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) Color(0xFF1976D2) else Color(0xFFE0E0E0),
                                contentColor = if (isActive) Color.White else Color.Gray
                            )
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OccupancyPriceRow(occupancy: String, label: String?, price: String, enabled: Boolean, onPriceChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
            Text(occupancy, modifier = Modifier.padding(start = 4.dp), fontWeight = FontWeight.Medium)
            if (label != null) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(label, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.Gray)
                }
            }
            Icon(Icons.Default.KeyboardArrowUp, null, tint = Color(0xFF1976D2), modifier = Modifier.padding(start = 8.dp).size(20.dp))
        }

        OutlinedTextField(
            value = price,
            onValueChange = { if (it.all { char -> char.isDigit() }) onPriceChange(it) },
            modifier = Modifier.width(150.dp),
            prefix = { Text("Rs. ", fontSize = 14.sp, color = Color.Gray) },
            enabled = enabled,
            shape = RoundedCornerShape(4.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = Color(0xFFF8F9FA),
                disabledBorderColor = Color(0xFFE2E8F0)
            )
        )
    }
}

@Composable
fun RestrictionRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.UnfoldMore, null, modifier = Modifier.padding(start = 8.dp).size(18.dp), tint = Color.Gray)
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
