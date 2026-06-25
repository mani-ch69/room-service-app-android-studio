package com.example.roomservice.ui.auth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionsRequestScreen(
    onPermissionsGranted: () -> Unit
) {
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // We proceed after the user makes a choice
        onPermissionsGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color(0xFFE3F2FD)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xFF1976D2)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Permission Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "To provide you the best experience and all features, Room Service Admin needs the following permissions:",
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(40.dp))

        // Permission List
        PermissionInfoItem(Icons.Default.Contacts, "Contacts", "To easily add and manage staff from your list.")
        PermissionInfoItem(Icons.Default.LocationOn, "Location", "To track service requests within hotel premises.")
        PermissionInfoItem(Icons.Default.Call, "Call & SMS", "To communicate with guests and staff instantly.")
        PermissionInfoItem(Icons.Default.Notifications, "Notifications", "To get real-time alerts for new orders & calls.")

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { launcher.launch(permissionsToRequest.toTypedArray()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            Text("ALLOW PERMISSIONS", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PermissionInfoItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Text(description, fontSize = 13.sp, color = Color.Gray)
        }
    }
}
