package com.example.roomservice.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.roomservice.util.BiometricHelper

@Composable
fun UnlockScreen(
    userName: String,
    userEmail: String,
    savedPin: String,
    useBiometric: Boolean,
    onUnlockSuccess: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Handle Back Button
    BackHandler {
        if (onBack != null) {
            onBack()
        } else {
            // Default behavior for startup lock: Minimize app instead of closing/popping
            activity?.moveTaskToBack(true)
        }
    }

    // Auto-trigger biometric if enabled
    LaunchedEffect(Unit) {
        if (useBiometric && activity != null) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                onSuccess = { 
                    pinInput = savedPin
                    onUnlockSuccess() 
                },
                onError = { /* User can still use PIN */ }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        Color(0xFF0D1233)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header
            Box(modifier = Modifier.fillMaxWidth()) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
                
                Text(
                    text = "ENTER MPIN",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // User Info
            Text(
                text = userName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = userEmail,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // MPIN Dots (6 Digits)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val isFilled = index < pinInput.length
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
                                if (isFilled) Color(0xFFC5A059) else Color.White.copy(alpha = dotAlpha)
                            )
                    )
                }
            }

            if (isError) {
                Text(
                    "Incorrect PIN", 
                    color = Color.Red, 
                    fontSize = 12.sp, 
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Forgot MPIN
            Text(
                text = "Forgot MPIN?",
                color = Color(0xFF64B5F6),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLogout() }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Custom Number Pad
            NumberPad(
                onNumberClick = { num ->
                    if (pinInput.length < 6) {
                        pinInput += num
                        isError = false
                        if (pinInput.length == 6) {
                            if (pinInput == savedPin) onUnlockSuccess()
                            else {
                                isError = true
                                pinInput = ""
                            }
                        }
                    }
                },
                onDeleteClick = {
                    if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                },
                onBiometricClick = {
                    activity?.let {
                        BiometricHelper.showBiometricPrompt(
                            activity = it,
                            onSuccess = { 
                                pinInput = savedPin
                                onUnlockSuccess() 
                            },
                            onError = { }
                        )
                    }
                },
                showBiometric = useBiometric
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Staff Control",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(By proceeding you agree to all T&C, Privacy Policy & Security Tips)",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 8.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(180.dp)
                )
            }
        }
    }
}

@Composable
fun NumberPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onBiometricClick: () -> Unit,
    showBiometric: Boolean
) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
    
    Column(
        modifier = Modifier.width(280.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rows 1-3
        for (i in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                for (j in 1..3) {
                    val num = (i * 3 + j).toString()
                    PadButton(text = num, onClick = { onNumberClick(num) }, modifier = Modifier.weight(1f))
                }
            }
        }
        
        // Bottom Row (Fingerprint, 0, Backspace)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fingerprint Icon
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.5f),
                contentAlignment = Alignment.Center
            ) {
                if (showBiometric) {
                    IconButton(onClick = onBiometricClick) {
                        Icon(Icons.Default.Fingerprint, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            
            // Number 0
            PadButton(text = "0", onClick = { onNumberClick("0") }, modifier = Modifier.weight(1f))
            
            // Backspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.5f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun PadButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.5f),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
