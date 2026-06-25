package com.example.roomservice.ui.guest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.BusinessDetailsRepository

@Composable
fun GuestLoginScreen(
    roomNumber: String,
    onLoginSuccess: (String) -> Unit
) {
    val businessDetails by BusinessDetailsRepository.details.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // 1. HOTEL LOGO & NAME
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color(0xFFE8F5E9)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(40.dp))
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = businessDetails.hotelName,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
        
        Text(
            text = "Welcome to your stay",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(60.dp))

        // 2. ROOM CARD (Displays the room from QR)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Hotel, null, tint = Color(0xFF1976D2))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("ROOM NUMBER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(roomNumber, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 3. PHONE NUMBER INPUT
        Text(
            "Enter Mobile Number",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(12.dp))
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { if (it.length <= 10) phoneNumber = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("00000 00000", color = Color.LightGray) },
            prefix = { Text("+91 ", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1976D2),
                unfocusedBorderColor = Color(0xFFDDDDDD)
            )
        )

        Spacer(Modifier.weight(1f))

        // 4. CONTINUE BUTTON
        Button(
            onClick = { 
                isProcessing = true
                onLoginSuccess(phoneNumber) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
            enabled = phoneNumber.length == 10 && !isProcessing
        ) {
            if (isProcessing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("START SESSION", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            "By continuing, you agree to our Terms of Service",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
