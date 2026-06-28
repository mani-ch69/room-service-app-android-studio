package com.example.roomservice.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500) 
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GANGA HOMES",
                color = Color(0xFF1976D2),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "HOME STAY'S CHAIN",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
