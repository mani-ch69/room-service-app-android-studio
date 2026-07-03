package com.example.roomservice.ui.guest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.BusinessDetailsRepository

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.example.roomservice.data.HotelSession

@Composable
fun GuestWebAppPreviewScreen() {
    val businessDetails by BusinessDetailsRepository.details.collectAsState()
    val hotelId by HotelSession.hotelId.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    
    // Updated URL to match the actual Render project URL
    val baseWebUrl = "https://room-service-portal.onrender.com"
    val guestAppUrl = "$baseWebUrl/?hotel=${hotelId ?: "DEMO"}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // HEADER
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Back */ }) { Icon(Icons.Default.ArrowBack, null) }
                Text(
                    text = "Guest App Preview", 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Share your Digital Menu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Guests can scan this QR or use the link to order from their room.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Placeholder for QR Code
                        Surface(
                            modifier = Modifier.size(200.dp),
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.QrCode, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                    Text("QR CODE", color = Color.LightGray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // URL Link Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF8FAF4),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = guestAppUrl,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                                TextButton(onClick = { clipboardManager.setText(AnnotatedString(guestAppUrl)) }) {
                                    Text("COPY", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { /* Open in browser */ },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1e40af))
                ) {
                    Icon(Icons.Default.OpenInBrowser, null)
                    Spacer(Modifier.width(10.dp))
                    Text("OPEN LIVE PREVIEW", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GuestActionCard(label: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp), tint = tint)
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun StatusPreviewCard(title: String, status: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFF1976D2), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(status, color = if(status == "Pending") Color.Red else Color(0xFF2E7D32), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title, 
        fontSize = 16.sp, 
        fontWeight = FontWeight.ExtraBold, 
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}
