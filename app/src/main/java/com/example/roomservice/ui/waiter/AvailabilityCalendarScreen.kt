package com.example.roomservice.ui.waiter

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BookingStatus
import com.example.roomservice.data.model.Room
import com.example.roomservice.ui.common.DateDisplayBox
import com.example.roomservice.ui.common.CommonDateRangePicker
import com.example.roomservice.ui.common.DateRangeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityCalendarScreen(
    rooms: List<Room>,
    bookings: List<Booking>,
    onBackClick: () -> Unit,
    onAddBooking: (Booking) -> Unit = {}
) {
    val roomTypes = remember(rooms) { rooms.map { it.roomType }.distinct().sorted() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        if (rooms.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No rooms created yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(roomTypes, key = { it }) { type ->
                    val roomsOfType = remember(type, rooms) { rooms.filter { it.roomType == type } }
                    TypeAvailabilityCalendar(
                        type = type,
                        rooms = roomsOfType,
                        bookings = bookings,
                        onAddBooking = onAddBooking
                    )
                }
            }
        }
    }
}

@Composable
fun TypeAvailabilityCalendar(
    type: String,
    rooms: List<Room>,
    bookings: List<Booking>,
    onAddBooking: (Booking) -> Unit
) {
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var showBulkEdit by remember { mutableStateOf(false) }
    val sdfMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header for Room Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = type, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${rooms.size} Rooms", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            onClick = { showBulkEdit = true },
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Bulk Edit",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val newMonth = calendarMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, -1)
                        calendarMonth = newMonth
                    }) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Color.Black)
                    }
                    Text(
                        text = sdfMonth.format(calendarMonth.time),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = {
                        val newMonth = calendarMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, 1)
                        calendarMonth = newMonth
                    }) {
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Black)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Day Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Calendar Grid for this type
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
                                
                                // Optimization: derivedStateOf for day status
                                val bookedRoomsCount by remember(rooms, bookings, date) {
                                    derivedStateOf {
                                        rooms.count { room ->
                                            bookings.any { b ->
                                                val checkIn = Calendar.getInstance().apply { timeInMillis = b.checkInDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                                                val checkOut = Calendar.getInstance().apply { timeInMillis = b.checkOutDate; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
                                                b.roomNumber == room.roomNumber && cal.timeInMillis >= checkIn && cal.timeInMillis <= checkOut && b.status != BookingStatus.CANCELLED
                                            }
                                        }
                                    }
                                }
                                
                                val isFullyBooked = bookedRoomsCount >= rooms.size && rooms.isNotEmpty()
                                val isPartiallyBooked = bookedRoomsCount > 0 && bookedRoomsCount < rooms.size

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isFullyBooked -> Color.Red.copy(alpha = 0.1f)
                                                isPartiallyBooked -> Color(0xFFFFA000).copy(alpha = 0.1f)
                                                else -> Color.Transparent
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = when {
                                                isFullyBooked -> Color.Red
                                                isToday -> MaterialTheme.colorScheme.primary
                                                else -> Color.Black
                                            }
                                        )
                                        if (isToday) {
                                            Box(Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                        }
                                        if (bookedRoomsCount > 0) {
                                            Text(
                                                text = "${rooms.size - bookedRoomsCount} left",
                                                fontSize = 8.sp,
                                                color = if (isFullyBooked) Color.Red else Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (week.size < 7) repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(8.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(Color.Red.copy(alpha = 0.1f), "Full")
                LegendItem(Color(0xFFFFA000).copy(alpha = 0.1f), "Partial")
                LegendItem(Color.Transparent, "Available")
            }
        }
    }

    if (showBulkEdit) {
        BulkEditDialog(
            roomType = type,
            rooms = rooms,
            onDismiss = { showBulkEdit = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkEditDialog(
    roomType: String,
    rooms: List<Room>,
    onDismiss: () -> Unit
) {
    var fromDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var toDate by remember { mutableLongStateOf(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000L)) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    
    var showRangePicker by remember { mutableStateOf(false) }

    var expandedSection by remember { mutableStateOf<String?>(null) }
    
    var roomsToSell by remember { mutableStateOf(rooms.size.toString()) }
    var price by remember { mutableStateOf("") }
    var roomStatusOpen by remember { mutableStateOf(true) }
    var selectedRatePlan by remember { mutableStateOf("Standard rate (2 guests)") }

    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF1F5F9)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Bulk edit", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Date and Days Section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DateDisplayBox(
                                        label = "From *",
                                        value = df.format(Date(fromDate)),
                                        onClick = { showRangePicker = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                    DateDisplayBox(
                                        label = "To *",
                                        value = df.format(Date(toDate)),
                                        onClick = { showRangePicker = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                Text("Which days of the week do you want to apply changes to?", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                
                                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                Column {
                                    days.chunked(4).forEach { rowDays ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            rowDays.forEach { day ->
                                                val dayIdx = days.indexOf(day) + 1
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Checkbox(
                                                        checked = selectedDays.contains(dayIdx),
                                                        onCheckedChange = { 
                                                            selectedDays = if (it) selectedDays + dayIdx else selectedDays - dayIdx
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                                    )
                                                    Text(day, fontSize = 11.sp, color = Color.Black)
                                                }
                                            }
                                            if (rowDays.size < 4) repeat(4 - rowDays.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Room Type Tabs Header
                    item {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                Text(
                                    text = roomType,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.width(24.dp))
                                Text(
                                    text = "Multiple room types",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(100.dp))
                        }
                    }

                    // 3. Expandable Sections
                    item {
                        BulkEditExpandableSection(
                            title = "Rooms to Sell",
                            subtitle = "Update the number of rooms to sell for this room type",
                            isExpanded = expandedSection == "rooms",
                            onToggle = { expandedSection = if (expandedSection == "rooms") null else "rooms" }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = roomsToSell,
                                    onValueChange = { 
                                        val num = it.toIntOrNull()
                                        if (it.isEmpty() || (num != null && num <= rooms.size)) roomsToSell = it
                                    },
                                    label = { Text("Room(s)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    suffix = { Text("Room(s)") },
                                    supportingText = { Text("Max ${rooms.size} rooms in property details") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Text("Changes will be made to the date range: ${df.format(Date(fromDate))} - ${df.format(Date(toDate))}", fontSize = 11.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = { expandedSection = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Save changes") }
                                    OutlinedButton(onClick = { expandedSection = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Cancel") }
                                }
                            }
                        }
                    }

                    item {
                        BulkEditExpandableSection(
                            title = "Prices",
                            subtitle = "Edit the prices of any rate plans for this room",
                            isExpanded = expandedSection == "prices",
                            onToggle = { expandedSection = if (expandedSection == "prices") null else "prices" }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                var rateExp by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = rateExp, onExpandedChange = { rateExp = !rateExp }) {
                                    OutlinedTextField(
                                        value = selectedRatePlan,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rateExp) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(expanded = rateExp, onDismissRequest = { rateExp = false }) {
                                        DropdownMenuItem(text = { Text("Standard rate (2 guests)") }, onClick = { selectedRatePlan = "Standard rate (2 guests)"; rateExp = false })
                                    }
                                }
                                
                                OutlinedTextField(
                                    value = price,
                                    onValueChange = { price = it },
                                    label = { Text("Price") },
                                    trailingIcon = { Text("₹", modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                
                                Text("Your occupancy-based prices for this rate plan will automatically be updated.", fontSize = 11.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = { expandedSection = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Save changes") }
                                    OutlinedButton(onClick = { expandedSection = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Cancel") }
                                }
                            }
                        }
                    }

                    item {
                        BulkEditExpandableSection(
                            title = "Room Status",
                            subtitle = "Open or close this room",
                            isExpanded = expandedSection == "status",
                            onToggle = { expandedSection = if (expandedSection == "status") null else "status" }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = roomStatusOpen, onClick = { roomStatusOpen = true })
                                    Text("Open Room", color = Color.Black)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = !roomStatusOpen, onClick = { roomStatusOpen = false })
                                    Text("Close Room", color = Color.Black)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = { expandedSection = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Save changes") }
                                    OutlinedButton(onClick = { expandedSection = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Cancel") }
                                }
                            }
                        }
                    }

                    item {
                        BulkEditExpandableSection(
                            title = "Restrictions",
                            subtitle = "Edit, add or remove restrictions for any rate plan for this room",
                            isExpanded = expandedSection == "restrictions",
                            onToggle = { expandedSection = if (expandedSection == "restrictions") null else "restrictions" }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                var resExp by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = resExp, onExpandedChange = { resExp = !resExp }) {
                                    OutlinedTextField(
                                        value = "Select a rate plan",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(resExp) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    ExposedDropdownMenu(expanded = resExp, onDismissRequest = { resExp = false }) {
                                        DropdownMenuItem(text = { Text("Standard rate") }, onClick = { resExp = false })
                                    }
                                }
                                
                                TextButton(onClick = { /* Add more logic */ }) {
                                    Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add more")
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(onClick = { expandedSection = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Save changes") }
                                    OutlinedButton(onClick = { expandedSection = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Cancel") }
                                }
                            }
                        }
                    }
                    
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (showRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = fromDate,
            initialSelectedEndDateMillis = toDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return DateRangeUtils.isSelectableFromToday(utcTimeMillis)
                }
            }
        )
        CommonDateRangePicker(
            state = dateRangePickerState,
            onDismiss = { showRangePicker = false },
            onConfirm = { start, end ->
                start?.let { fromDate = it }
                end?.let { toDate = it }
            }
        )
    }
}

@Composable
fun BulkEditExpandableSection(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE), modifier = Modifier.padding(bottom = 16.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)).let { if(color == Color.Transparent) it.background(Color.White).border(0.5.dp, Color.LightGray, RoundedCornerShape(2.dp)) else it })
        Spacer(Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
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
