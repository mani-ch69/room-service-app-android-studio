package com.example.roomservice.ui.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SecuritySetupScreen(
    onComplete: (pin: String, biometric: Boolean) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var showBiometricStep by remember { mutableStateOf(false) }

    if (!showBiometricStep) {
        // STEP 1: MPIN SETUP
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)), // Light Gray Background
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR
            Surface(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Security Setup", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF1976D2) // Professional Blue
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "SET UP MPIN",
                color = Color.Black,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Create a 6-digit security PIN for your account",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // MPIN Dots with Animation
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val isFilled = index < pin.length
                    val dotScale by animateFloatAsState(
                        targetValue = if (isFilled) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dot_scale"
                    )
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (isFilled) 1f else 0.5f,
                        label = "dot_alpha"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer(scaleX = dotScale, scaleY = dotScale)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) Color(0xFF1976D2) else Color.LightGray.copy(alpha = dotAlpha)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Spacer(modifier = Modifier.weight(1f))

            // NUMBER PAD
            Column(
                modifier = Modifier.width(280.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val numRows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
                for (row in numRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        for (num in row) {
                            Surface(
                                onClick = { if (pin.length < 6) pin += num },
                                modifier = Modifier.weight(1f).aspectRatio(1.5f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 1.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = num, color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        onClick = { if (pin.length < 6) pin += "0" },
                        modifier = Modifier.weight(1f).aspectRatio(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "0", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).aspectRatio(1.5f), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // NEXT BUTTON
            Button(
                onClick = { 
                    if (pin.length == 6) {
                        showBiometricStep = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                enabled = pin.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("CONTINUE", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }

            TextButton(
                onClick = { onComplete("", false) },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("SKIP SECURITY (NOT RECOMMENDED)", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    } else {
        // STEP 2: BIOMETRIC SETUP
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color(0xFFE8F5E9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFF2E7D32)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Enable Biometric Login",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Use your fingerprint or face recognition for faster and more secure access to the app.",
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { onComplete(pin, true) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("ENABLE BIOMETRIC", fontWeight = FontWeight.ExtraBold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = { onComplete(pin, false) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SKIP FOR NOW", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}
