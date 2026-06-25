package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BookingStatus
import com.example.roomservice.data.model.Room
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
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val sdf = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
    
    var showAddBookingDialog by remember { mutableStateOf<Room?>(null) }
    var showFullDatePicker by remember { mutableStateOf(false) }

    if (showFullDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showFullDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showFullDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showFullDatePicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Room Availability", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { showFullDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF1F5F9))) {
            // Calendar Strip (Simplified)
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sdf.format(Date(selectedDate)), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { showFullDatePicker = true }) {
                            Text("Change Date")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in -1..5) { // Show yesterday, today, and 5 days ahead
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = System.currentTimeMillis()
                            cal.add(Calendar.DAY_OF_YEAR, i)
                            
                            val isSelected = SimpleDateFormat("ddMM", Locale.getDefault()).format(cal.time) == 
                                            SimpleDateFormat("ddMM", Locale.getDefault()).format(Date(selectedDate))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedDate = cal.timeInMillis }
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time),
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else Color.Gray
                                )
                                Text(
                                    text = SimpleDateFormat("dd", Locale.getDefault()).format(cal.time),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }

            Text(
                "Room Status for Selected Date",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            if (rooms.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No rooms created yet.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rooms.size) { index ->
                        val room = rooms[index]
                        val booking = bookings.find { b -> 
                            val checkIn = Calendar.getInstance().apply { timeInMillis = b.checkInDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                            val checkOut = Calendar.getInstance().apply { timeInMillis = b.checkOutDate; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
                            
                            b.roomNumber == room.roomNumber && 
                            selectedDate >= checkIn && 
                            selectedDate <= checkOut &&
                            b.status != BookingStatus.CANCELLED
                        }

                        RoomAvailabilityCard(room, booking, onClick = {
                            if (booking == null) {
                                showAddBookingDialog = room
                            }
                        })
                    }
                }
            }
        }

        if (showAddBookingDialog != null) {
            // Reusing AddBookingDialog from BookingManagementScreen if possible, 
            // but since it's in another file, I might need to make it public or duplicate it.
            // For now, I'll assume it's accessible or I'll call a shared component if I refactor.
            // Actually, AddBookingDialog is in BookingManagementScreen.kt and it is NOT private.
            AddBookingDialog(
                rooms = rooms,
                onDismiss = { showAddBookingDialog = null },
                onConfirm = { 
                    onAddBooking(it)
                    showAddBookingDialog = null
                }
            )
        }
    }
}

@Composable
fun RoomAvailabilityCard(room: Room, activeBooking: Booking?, onClick: () -> Unit = {}) {
    val isBooked = activeBooking != null
    val color = if (isBooked) Color.Red else Color(0xFF2E7D32)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = color) {}
                Spacer(Modifier.width(8.dp))
                Text("Room ${room.roomNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Text(room.roomType, fontSize = 12.sp, color = Color.Gray)
            
            Spacer(Modifier.height(12.dp))
            
            if (isBooked) {
                Text("Booked by:", fontSize = 10.sp, color = Color.Gray)
                Text(activeBooking!!.guestName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(activeBooking.status.name, fontSize = 10.sp, color = color, fontWeight = FontWeight.Black)
            } else {
                Text("AVAILABLE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = color)
                Text("Tap to Book", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
