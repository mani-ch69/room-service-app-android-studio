package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.BusinessDetailsRepository

@Composable
fun GeneralInfoStatusScreen() {
    val businessDetails by BusinessDetailsRepository.details.collectAsState()
    var isPropertyOpen by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "General Info",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // General Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Property Name
                Column {
                    Text(
                        text = "Property name:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = businessDetails.hotelName,
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Change property name",
                        fontSize = 13.sp,
                        color = Color(0xFF0066CC),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { /* TODO */ }
                            .padding(top = 4.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Property Address
                Column {
                    Text(
                        text = "Property address:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = businessDetails.address,
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Property Location
                Column {
                    Text(
                        text = "Property location:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "25.3250887844738, 83.0053562215121 (on Google Maps and OpenStreetMap)",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        // Property Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Property status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Surface(
                    color = if (isPropertyOpen) Color(0xFF2E7D32) else Color.Red,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isPropertyOpen) "Open/Bookable" else "Closed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Guests can see and book your property. If you want to pause, you can close your property temporarily without changing anything in your calendar and schedule your reopening.",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = { isPropertyOpen = !isPropertyOpen },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPropertyOpen) Color(0xFF0066CC) else Color(0xFF2E7D32)
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = if (isPropertyOpen) "Close temporarily" else "Open Property",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
