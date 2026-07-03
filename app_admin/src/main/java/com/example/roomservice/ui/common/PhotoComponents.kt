package com.example.roomservice.ui.common

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Reusable Photo Gallery Component
 * Can be used for Main Gallery or Room-specific Photos
 */
@Composable
fun PhotoGallerySection(
    title: String,
    photoCount: Int,
    photos: List<String>,
    onAddClick: () -> Unit,
    onDeleteClick: (String) -> Unit = {},
    showActions: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title ($photoCount photos)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                if (showActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Select all", fontSize = 11.sp, color = Color(0xFF1976D2), modifier = Modifier.clickable { })
                        Text("Delete", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.clickable { })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (title.contains("Main")) {
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add photos", fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
            }

            // Responsive Grid for Photos
            // In a real app, we would use a dynamic list. Here we show a preview grid.
            val columns = 3
            val spacing = 8.dp
            
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                val chunks = (photos + "ADD_BUTTON").chunked(columns)
                chunks.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        rowItems.forEach { item ->
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                if (item == "ADD_BUTTON") {
                                    AddPhotoPlaceholder(onAddClick)
                                } else {
                                    PhotoItem(url = item)
                                }
                            }
                        }
                        // Fill remaining space if row is not full
                        if (rowItems.size < columns) {
                            repeat(columns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoItem(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun AddPhotoPlaceholder(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF1976D2), modifier = Modifier.size(24.dp))
            Text("Add photos", color = Color(0xFF1976D2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
