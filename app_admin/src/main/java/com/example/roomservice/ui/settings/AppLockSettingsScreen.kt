package com.example.roomservice.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalContext
import com.example.roomservice.util.SecurityManager
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roomservice.ui.auth.UnlockScreen

@Composable
fun AppLockSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    var showVerification by remember { mutableStateOf(false) }
    var verificationPurpose by remember { mutableStateOf("") } // "MPIN" or "BIOMETRIC"
    
    var showNewPinSetup by remember { mutableStateOf(false) }
    var showBiometricActivation by remember { mutableStateOf(false) }
    var showDisableLockConfirmation by remember { mutableStateOf(false) }
    
    val userData = securityManager.getUserData()
    var isLockEnabled by remember { mutableStateOf(securityManager.isLockEnabled()) }
    var biometricEnabled by remember { mutableStateOf(securityManager.useBiometric()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color(0xFFF1F5F9)) {
                                Icon(Icons.Default.LockOpen, null, tint = Color.Gray, modifier = Modifier.padding(10.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("None", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Disable all app security", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        RadioButton(
                            selected = !isLockEnabled,
                            onClick = { 
                                if (isLockEnabled) {
                                    verificationPurpose = "DISABLE"
                                    showVerification = true
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color(0xFFE8F5E9)) {
                                Icon(Icons.Default.Pin, null, tint = Color(0xFF2E7D32), modifier = Modifier.padding(10.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("MPIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("6-digit security PIN", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        RadioButton(
                            selected = isLockEnabled,
                            onClick = { 
                                if (!isLockEnabled) {
                                    showNewPinSetup = true
                                } else {
                                    // Already enabled, maybe update PIN?
                                    verificationPurpose = "MPIN"
                                    showVerification = true
                                }
                            }
                        )
                    }
                    
                    if (isLockEnabled) {
                        TextButton(
                            onClick = { 
                                verificationPurpose = "MPIN"
                                showVerification = true 
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("CHANGE PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (isLockEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color(0xFFE3F2FD)) {
                                Icon(Icons.Default.Fingerprint, null, tint = Color(0xFF1976D2), modifier = Modifier.padding(10.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Biometric Lock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(if (biometricEnabled) "Enabled" else "Configure Fingerprint", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    verificationPurpose = "BIOMETRIC"
                                    showVerification = true 
                                } else {
                                    securityManager.setAppLock(securityManager.getPin(), false)
                                    biometricEnabled = false
                                    Toast.makeText(context, "Biometric Disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1976D2))
                        )
                    }
                }
            }
        }
    }

    if (showVerification) {
        Dialog(
            onDismissRequest = { showVerification = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val email = userData["email"] ?: ""
            val maskedEmail = if (email.contains("@")) {
                "${email[0]}***${email.substringAfter("@")}"
            } else email

            UnlockScreen(
                userName = userData["name"] ?: "Admin",
                userEmail = maskedEmail,
                savedPin = securityManager.getPin(),
                useBiometric = securityManager.useBiometric(),
                onUnlockSuccess = {
                    showVerification = false
                    when (verificationPurpose) {
                        "MPIN" -> showNewPinSetup = true
                        "BIOMETRIC" -> showBiometricActivation = true
                        "DISABLE" -> {
                            securityManager.disableLock()
                            isLockEnabled = false
                            biometricEnabled = false
                            Toast.makeText(context, "App Lock Disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onLogout = { showVerification = false },
                onBack = { showVerification = false }
            )
        }
    }

    if (showNewPinSetup) {
        NewPinSetupDialog(
            onDismiss = { showNewPinSetup = false },
            onConfirm = { newPin ->
                securityManager.setAppLock(newPin, securityManager.useBiometric())
                isLockEnabled = true
                showNewPinSetup = false
                Toast.makeText(context, "Security Lock Updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showBiometricActivation) {
        BiometricActivationDialog(
            onDismiss = { 
                showBiometricActivation = false
                biometricEnabled = false 
            },
            onEnable = {
                securityManager.setAppLock(securityManager.getPin(), true)
                biometricEnabled = true
                showBiometricActivation = false
                Toast.makeText(context, "Biometric Enabled Successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun BiometricActivationDialog(onDismiss: () -> Unit, onEnable: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("Enable Biometric Lock", fontWeight = FontWeight.Bold) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Ensure that your own fingerprint is registered on this device",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Some times will still need MPIN authorisation",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Text(
                    "You can disable this anytime through app settings",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEnable) {
                Text("ENABLE", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Red)
            }
        }
    )
}

@Composable
fun NewPinSetupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Set New MPIN" else "Confirm New MPIN") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Enter a 6-digit PIN for your app lock")
                Spacer(modifier = Modifier.height(16.dp))
                val currentInput = if (step == 1) pin else confirmPin
                
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            if (step == 1) pin = it else confirmPin = it
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.width(200.dp),
                    label = { Text("6-Digit PIN") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1 && pin.length == 6) {
                        step = 2
                    } else if (step == 2 && confirmPin == pin) {
                        onConfirm(pin)
                    }
                },
                enabled = if (step == 1) pin.length == 6 else confirmPin.length == 6
            ) {
                Text(if (step == 1) "NEXT" else "UPDATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
