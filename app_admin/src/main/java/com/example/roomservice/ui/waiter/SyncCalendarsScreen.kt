package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.RoomRepository

@Composable
fun SyncCalendarsScreen() {
    val rooms by RoomRepository.rooms.collectAsState()
    val roomTypes = remember(rooms) { rooms.map { it.roomType }.distinct() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Sync your calendars with Airbnb, VRBO, and more",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Sync your calendars to view all your reservations and availability across sites in one place and prevent double bookings.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Dynamic Room Type Sections
            items(roomTypes) { type ->
                CalendarRoomSection(type)
            }

            // Statuses Explained Section (Mobile optimized version of sidebar)
            item {
                StatusesExplainedCard()
            }

            // Export Preferences Section
            item {
                ExportPreferencesCard()
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun CalendarRoomSection(roomType: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = roomType, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            
            Spacer(Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                    modifier = Modifier.height(36.dp).weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Add calendar connection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2)),
                    modifier = Modifier.height(36.dp).weight(0.8f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp), tint = Color(0xFF1976D2))
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh", fontSize = 11.sp, color = Color(0xFF1976D2))
                }
            }

            // Mock table for active connections (if any)
            if (roomType.contains("Budget")) {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Calendar name", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("Status", modifier = Modifier.weight(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("calendar.google.com", modifier = Modifier.weight(1f), fontSize = 11.sp)
                        Surface(color = Color(0xFFE6FFFA), shape = RoundedCornerShape(4.dp)) {
                            Text("Okay", color = Color(0xFF047857), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Remove", color = Color.Red, fontSize = 10.sp, modifier = Modifier.clickable { })
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Avoid manually blocking off dates in all your calendars and limit double bookings by importing reservations from other platforms.",
                    fontSize = 11.sp, color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StatusesExplainedCard() {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Statuses explained", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Icon(if(expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
            }
            
            if (expanded) {
                Spacer(Modifier.height(16.dp))
                StatusInfoRow("Okay", "Your import and export connections are working perfectly fine.", Color(0xFF10B981))
                StatusInfoRow("Activating", "We're setting up your connection.", Color(0xFFF59E0B))
                StatusInfoRow("Import only", "We're only importing bookings.", Color(0xFFF59E0B))
                StatusInfoRow("Service error", "There's a problem with the connection.", Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun StatusInfoRow(label: String, desc: String, color: Color) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Surface(color = color, shape = RoundedCornerShape(4.dp)) {
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Text(desc, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
    }
}

@Composable
fun ExportPreferencesCard() {
    var selectedOption by remember { mutableStateOf("booked_dates") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Decide what to export", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "Stay on top of reservations and limit double bookings by exporting both your Booking.com reservations and closed dates to other sites.",
                fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedOption == "booked_closed", onClick = { selectedOption = "booked_closed" })
                Text("Booked and closed dates", fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedOption == "booked_dates", onClick = { selectedOption = "booked_dates" })
                Text("Booked dates only", fontSize = 13.sp)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
