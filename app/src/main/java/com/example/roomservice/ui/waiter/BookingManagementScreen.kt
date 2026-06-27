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

    val sdf = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }

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
        // FILTER HEADER
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // PRIMARY FILTERS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterDropdownField("Date of", listOf("Reservation", "Check-in", "Check-out", "Invoice", "Stay"), dateOfType) { dateOfType = it }
                    
                    DatePickerField("From", sdf.format(Date(dateFrom)), Modifier.weight(1f)) { dateFrom = it }
                    DatePickerField("Until", sdf.format(Date(dateTo)), Modifier.weight(1f)) { dateTo = it }
                    
                    OutlinedButton(
                        onClick = { isMoreFiltersExpanded = !isMoreFiltersExpanded },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1976D2)),
                        border = BorderStroke(1.dp, Color(0xFF1976D2)),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("More filters")
                        Icon(if(isMoreFiltersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                    
                    Button(
                        onClick = { applyFilters() },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Show")
                    }
                }

                // MORE FILTERS PANEL
                AnimatedVisibility(visible = isMoreFiltersExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterGroup("Reservation Status", listOf("Ok", "Canceled", "No-show", "Corporate card"), selectedStatuses, Modifier.weight(1f))
                            FilterGroup("Guest Communication", listOf("Pending guest request", "Invoice required"), selectedComm, Modifier.weight(1f))
                            FilterGroup("Invalid credit card", listOf("Updated", "Pending"), selectedCredit, Modifier.weight(1f))
                            
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Guest name or booking number", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    placeholder = { Text("Search...", fontSize = 13.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // RESULTS LIST
        if (filteredBookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bookings match your filters.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    BookingTableHeader()
                }
                items(filteredBookings, key = { it.id }) { booking ->
                    DashboardBookingCardRedesigned(
                        booking = booking,
                        rooms = rooms,
                        selectedDate = System.currentTimeMillis(),
                        onDelete = onDeleteBooking
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdownField(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().width(140.dp),
                shape = RoundedCornerShape(4.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 13.sp) },
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
        Text(label, fontSize = 12.sp, color = Color.Gray)
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().clickable { showDialog = true },
            enabled = false,
            shape = RoundedCornerShape(4.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = Color.LightGray,
                disabledLabelColor = Color.Gray
            )
        )
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
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        options.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selectedList.contains(option),
                    onCheckedChange = { 
                        if (it) selectedList.add(option) else selectedList.remove(option)
                    },
                    modifier = Modifier.scale(0.8f)
                )
                Text(option, fontSize = 12.sp)
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
