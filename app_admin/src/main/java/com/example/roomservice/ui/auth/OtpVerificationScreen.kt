package com.example.roomservice.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    mobileNumber: String,
    verificationId: String,
    hotelName: String = "Room Service",
    onBackClick: () -> Unit,
    onVerifySuccess: (String) -> Unit
) {
    var otpValue by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically focus and show keyboard
    LaunchedEffect(Unit) {
        delay(500)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Trigger verification when OTP reaches 6 digits
    LaunchedEffect(otpValue) {
        if (otpValue.length == 6) {
            isVerifying = true
            val credential = PhoneAuthProvider.getCredential(verificationId, otpValue)
            signInWithCredential(auth, credential, {
                onVerifySuccess(mobileNumber)
            }, { error ->
                isVerifying = false
                otpValue = ""
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // TOP BAR
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFF1976D2)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(hotelName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1976D2))
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Spacer(Modifier.height(40.dp))

        // CONTENT
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Verify Mobile Number",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                "OTP has been sent to +91 $mobileNumber",
                fontSize = 15.sp,
                color = Color.Gray
            )
            
            Spacer(Modifier.height(40.dp))

            // OTP INPUT AREA
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = otpValue,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) otpValue = it 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    decorationBox = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(6) { index ->
                                val char = otpValue.getOrNull(index)?.toString() ?: ""
                                val isFocused = index == otpValue.length
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .border(
                                            width = if (isFocused) 2.dp else 1.dp,
                                            color = if (isFocused) Color(0xFF1976D2) else if (char.isNotEmpty()) Color(0xFF1976D2).copy(alpha = 0.5f) else Color.LightGray,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(if (char.isNotEmpty()) Color(0xFFE3F2FD).copy(alpha = 0.5f) else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            if (isVerifying) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Verifying OTP...", color = Color(0xFF1976D2), fontWeight = FontWeight.Medium)
                }
            } else {
                TextButton(
                    onClick = { 
                        Toast.makeText(context, "Resending OTP...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("RESEND OTP", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.weight(1f))

        Text(
            "Auto-verification is active. OTP will be captured automatically if possible.",
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

private fun signInWithCredential(
    auth: FirebaseAuth, 
    credential: PhoneAuthCredential,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    auth.signInWithCredential(credential).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            onSuccess()
        } else {
            onFailure(task.exception?.message ?: "Verification failed")
        }
    }
}
