package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.RoomRepository
import com.example.roomservice.ui.common.PhotoGallerySection

@Composable
fun PropertyPhotosScreen() {
    val rooms by RoomRepository.rooms.collectAsState()
    val roomTypes = remember(rooms) { rooms.map { it.roomType }.distinct() }

    // Mock data for preview (In a real app, these would come from Firebase Storage)
    val mainPhotos = remember { List(15) { "https://i.ibb.co/Xf7yZ8N/gh-stay-logo.jpg" } }
    val roomPhotosMap = remember(roomTypes) { 
        roomTypes.associateWith { List(4) { "https://i.ibb.co/Xf7yZ8N/gh-stay-logo.jpg" } } 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Property Photos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                
                // Tabs Mockup
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    PhotoTabItem("All photos", true)
                    PhotoTabItem("Room photos", false)
                }
                Spacer(Modifier.height(8.dp))
            }

            // Info Box
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "High quality photos help guests decide where to stay. Add more photos of your property to increase bookings.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Main Gallery Section
            item {
                PhotoGallerySection(
                    title = "Main gallery",
                    photoCount = mainPhotos.size,
                    photos = mainPhotos,
                    onAddClick = { /* Handle Add */ }
                )
            }

            // Dynamic Room Type Sections
            items(roomTypes) { type ->
                val photos = roomPhotosMap[type] ?: emptyList()
                PhotoGallerySection(
                    title = type,
                    photoCount = photos.size,
                    photos = photos,
                    onAddClick = { /* Handle Add for this room type */ }
                )
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PhotoTabItem(label: String, selected: Boolean) {
    Surface(
        color = if (selected) Color(0xFF1976D2).copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF1976D2) else Color.Gray
        )
    }
}
