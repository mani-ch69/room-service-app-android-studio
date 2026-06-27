package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.roomservice.data.model.*

@Composable
fun BookingManagementScreen(
    bookings: List<Booking>,
    rooms: List<Room>,
    onDeleteBooking: (String) -> Unit
) {
    // Optimization: Pre-sort once outside of the list items
    val sortedBookings = remember(bookings) {
        bookings.sortedByDescending { it.checkInDate }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (sortedBookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No manual bookings recorded.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Optimization: Use keys for lazy list performance
                items(sortedBookings, key = { it.id }) { booking ->
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
