package com.example.roomservice.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: (name: String, email: String, phone: String, pass: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Details, 2: Mobile Verification
    
    var adminName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A237E)) // App Deep Blue Theme
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Title sync with App Theme
        Text(
            text = if (step == 1) "Create Account" else "Verify Mobile",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        if (step == 1) {
            // STEP 1: ACCOUNT DETAILS (Hotel Section Removed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SignUpTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    label = "Admin Name",
                    icon = Icons.Default.Person
                )

                SignUpTextField(
                    value = emailAddress,
                    onValueChange = { emailAddress = it },
                    label = "Email Address",
                    icon = Icons.Default.Email
                )

                SignUpTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onVisibilityChange = { passwordVisible = !passwordVisible }
                )

                SignUpTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    passwordVisible = confirmPasswordVisible,
                    onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { step = 2 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)), // App Gold
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("NEXT: VERIFY MOBILE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account? ", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text(
                        text = "Login",
                        color = Color(0xFFC5A059),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { onBackToLogin() }
                    )
                }
            }
        } else {
            // STEP 2: MOBILE VERIFICATION (Sync with PhonePe layout style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Enter Your Mobile Number",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Please enter your mobile number for verification.",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { if (it.length <= 10) mobileNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = Color.White) },
                    placeholder = { Text("00000 00000", color = Color.White.copy(alpha = 0.3f)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFC5A059),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { 
                        if (mobileNumber.length == 10) {
                            onSignUpSuccess(adminName, emailAddress, mobileNumber, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    enabled = mobileNumber.length == 10,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC5A059),
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("VERIFY & CONTINUE", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = { step = 1 },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Edit Details", color = Color(0xFFC5A059), fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onVisibilityChange: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Color.White.copy(alpha = 0.4f)) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(icon, null, tint = Color(0xFFC5A059)) },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { onVisibilityChange?.invoke() }) {
                    Icon(image, null, tint = Color.White.copy(alpha = 0.4f))
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White.copy(alpha = 0.7f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
            cursorColor = Color(0xFFC5A059)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
