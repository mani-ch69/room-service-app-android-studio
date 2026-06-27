package com.example.roomservice.ui.waiter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingManagementScreen(
    bookings: List<Booking>,
    rooms: List<Room>,
    onDeleteBooking: (String) -> Unit
) {
    var dateOfType by remember { mutableStateOf("Check-in") }
    var dateFrom by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dateTo by remember { mutableLongStateOf(System.currentTimeMillis() + 86400000L) }
    var isMoreFiltersExpanded by remember { mutableStateOf(false) }
    
    // Status Filters
    val selectedStatuses = remember { mutableStateListOf<String>() }
    val selectedComm = remember { mutableStateListOf<String>() }
    val selectedCredit = remember { mutableStateListOf<String>() }
    var searchQuery by remember { mutableStateOf("") }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var filteredBookings by remember { mutableStateOf(bookings) }

    fun applyFilters() {
        filteredBookings = bookings.filter { b ->
            val dateToCompare = when (dateOfType) {
                "Check-in" -> b.checkInDate
                "Check-out" -> b.checkOutDate
                "Reservation" -> b.timestamp
                else -> b.checkInDate
            }

            val inDateRange = dateToCompare in (dateFrom..dateTo)
            val matchesStatus = selectedStatuses.isEmpty() || selectedStatuses.any { s ->
                when(s) {
                    "Ok" -> b.status == BookingStatus.BOOKED || b.status == BookingStatus.CHECKED_IN
                    "Canceled" -> b.status == BookingStatus.CANCELLED
                    else -> true
                }
            }
            val matchesSearch = searchQuery.isEmpty() || 
                             b.guestName.contains(searchQuery, ignoreCase = true) || 
                             b.bookingNumber.contains(searchQuery, ignoreCase = true)

            inDateRange && matchesStatus && matchesSearch
        }.sortedByDescending { it.checkInDate }
    }

    LaunchedEffect(bookings) { applyFilters() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        // MOBILE OPTIMIZED FILTER HEADER
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // PRIMARY FILTERS (Vertical Stack for Mobile)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterDropdownField("Date of", listOf("Reservation", "Check-in", "Check-out", "Invoice", "Stay"), dateOfType) { dateOfType = it }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatePickerField("From", sdf.format(Date(dateFrom)), Modifier.weight(1f)) { dateFrom = it }
                        DatePickerField("Until", sdf.format(Date(dateTo)), Modifier.weight(1f)) { dateTo = it }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { isMoreFiltersExpanded = !isMoreFiltersExpanded },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                            border = BorderStroke(1.dp, Color(0xFF1976D2))
                        ) {
                            Text("More filters", fontSize = 14.sp)
                            Icon(if(isMoreFiltersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        }
                        
                        Button(
                            onClick = { applyFilters() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.6f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Show", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // MORE FILTERS PANEL (Vertical Scrollable for Mobile)
                AnimatedVisibility(visible = isMoreFiltersExpanded) {
                    Column(modifier = Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Guest name or booking number", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                            singleLine = true
                        )

                        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                        FilterGroup("Reservation Status", listOf("Ok", "Canceled", "No-show"), selectedStatuses)
                        FilterGroup("Guest Communication", listOf("Pending request", "Invoice required"), selectedComm)
                    }
                }
            }
        }

        // RESULTS LIST
        if (filteredBookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("No results found", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Found ${filteredBookings.size} bookings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(filteredBookings, key = { it.id }) { booking ->
                    DashboardBookingCardRedesigned(
                        booking = booking,
                        rooms = rooms,
                        selectedDate = System.currentTimeMillis(),
                        onDelete = onDeleteBooking
                    )
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdownField(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1976D2),
                    unfocusedBorderColor = Color.LightGray
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DatePickerField(label: String, value: String, modifier: Modifier = Modifier, onDateSelected: (Long) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { showDialog = true }
                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = value, fontSize = 14.sp, color = Color.Black)
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
            }
        }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun FilterGroup(title: String, options: List<String>, selectedList: MutableList<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF334155))
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedList.contains(option)
                FilterChip(
                    selected = isSelected,
                    onClick = { if (isSelected) selectedList.remove(option) else selectedList.add(option) },
                    label = { Text(option) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1976D2),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun BookingTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HeaderText("Guest Name", Modifier.weight(1.5f))
        HeaderText("Check-in", Modifier.weight(1f))
        HeaderText("Check-out", Modifier.weight(1f))
        HeaderText("Status", Modifier.weight(1f))
        HeaderText("Booking #", Modifier.weight(1f))
    }
}

@Composable
fun HeaderText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = modifier)
}
