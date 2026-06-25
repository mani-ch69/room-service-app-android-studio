package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.ChatRepository
import com.example.roomservice.data.RoomRepository
import com.example.roomservice.data.model.Room
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminChatListScreen(
    onChatClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val messages by ChatRepository.messages.collectAsState()
    val rooms by RoomRepository.rooms.collectAsState()
    var showNewChatDialog by remember { mutableStateOf(false) }
    
    // Get all rooms and sort them by latest message timestamp
    val chatRooms = remember(messages, rooms) {
        rooms.map { room ->
            val lastTimestamp = messages.filter { it.roomNumber == room.roomNumber }
                .maxOfOrNull { it.timestamp } ?: 0L
            room to lastTimestamp
        }.sortedByDescending { it.second }
    }

    Scaffold(
        // Removed Floating Action Button as all rooms are now always visible in the list
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color.White)) {
            if (rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("No rooms created yet", color = Color.Gray)
                        Text("Add rooms in Hotel & Stay section", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chatRooms) { (room, _) ->
                        val roomNumber = room.roomNumber
                        val lastMsg = messages.filter { it.roomNumber == roomNumber }.maxByOrNull { it.timestamp }
                        WhatsAppChatItem(
                            roomNumber = roomNumber,
                            lastMsgText = lastMsg?.text ?: "No messages yet",
                            timestamp = lastMsg?.timestamp ?: 0L,
                            isAdminLast = lastMsg?.senderId == "admin",
                            onClick = { onChatClick(roomNumber) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 85.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start New Chat", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(rooms) { room ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    showNewChatDialog = false
                                    onChatClick(room.roomNumber)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color(0xFF128C7E).copy(alpha = 0.1f)) {
                                Box(contentAlignment = Alignment.Center) { Text(room.roomNumber, color = Color(0xFF128C7E), fontWeight = FontWeight.Bold) }
                            }
                            Spacer(Modifier.width(16.dp))
                            Text("Room ${room.roomNumber}", fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNewChatDialog = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
fun WhatsAppChatItem(
    roomNumber: String,
    lastMsgText: String,
    timestamp: Long,
    isAdminLast: Boolean,
    onClick: () -> Unit
) {
    val time = remember(timestamp) {
        if (timestamp == 0L) "" 
        else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Surface(
            modifier = Modifier.size(55.dp),
            shape = CircleShape,
            color = Color(0xFF128C7E).copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = roomNumber, 
                    fontWeight = FontWeight.Bold, 
                    color = Color(0xFF128C7E),
                    fontSize = 18.sp
                )
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Room $roomNumber", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 17.sp,
                    color = Color.Black
                )
                Text(
                    text = time, 
                    color = Color.Gray, 
                    fontSize = 12.sp
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdminLast) {
                    Icon(
                        Icons.Default.DoneAll, 
                        null, 
                        tint = Color(0xFF34B7F1), 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = lastMsgText, 
                    color = Color.Gray, 
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
