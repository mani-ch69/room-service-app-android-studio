package com.example.roomservice.ui.waiter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessagingPreferencesScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General settings", "Message templates", "Automatic replies", "Template scheduler", "Security settings")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        Column(modifier = Modifier.background(Color.White).padding(top = 16.dp)) {
            Text(
                "Messaging Preferences",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "Make changes to your guest templates, replies, and notifications from here",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            )

            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2),
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF1976D2)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = if(selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium) }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> GeneralSettingsTab()
                1 -> MessageTemplatesTab()
                2 -> AutomaticRepliesTab()
                3 -> TemplateSchedulerTab()
                4 -> SecuritySettingsTab()
            }
        }
    }
}

@Composable
fun GeneralSettingsTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Control the new message notifications we send to your reservations email address.", fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    NotificationSwitchRow("Email me when:", "A guest sends a message", true)
                    NotificationSwitchRow("", "A guest request gets an automatic reply", true)
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
                    Spacer(Modifier.height(16.dp))
                    
                    NotificationSwitchRow("Daily reminder email", "Stay on top of guest emails with our daily reminder. Get an email in the morning to catch up on pending messages from the previous day.", true)
                }
            }
        }
        item {
            Text("Receive messages on-the-go by enabling push notifications directly on the Pulse app.", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun NotificationSwitchRow(label: String, subLabel: String, initialState: Boolean) {
    var checked by remember { mutableStateOf(initialState) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            if (label.isNotEmpty()) Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subLabel, fontSize = 12.sp, color = if(label.isEmpty()) Color.Black else Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = checked, onCheckedChange = { checked = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1976D2)))
            Text(if(checked) "On" else "Off", fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun MessageTemplatesTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Templates", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Create, update, and manage all of your message templates", fontSize = 12.sp, color = Color.Gray)
                }
                Button(onClick = {}, shape = RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) {
                    Text("Create new template", fontSize = 12.sp)
                }
            }
        }
        item { TemplateCard("Reservation", "reservation", "Due to extremely high demand, we are currently not accepting bookings without payment. please pay 25% of the total amount to confirm . contact (+915423567944 / +919450872557 manish chaurasiya )") }
        item { TemplateCard("Cancellation", "unconfirm booking", "Your booking has not been confirmed as no payment was received. We apologize for the inconvenience.") }
    }
}

@Composable
fun TemplateCard(category: String, title: String, content: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(4.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(12.dp)) {
                Text(category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("English (UK)", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text(content, fontSize = 12.sp, color = Color.DarkGray)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AddCircle, null, tint = Color.Gray, modifier = Modifier.size(14.dp)); Text(" Add language", fontSize = 11.sp, color = Color.Gray) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(14.dp)); Text(" Delete template", fontSize = 11.sp, color = Color.Gray) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(14.dp)); Text(if(category=="Reservation") " When a guest makes a booking" else " Schedule template", fontSize = 11.sp, color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
fun AutomaticRepliesTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Automatic Replies", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Read our Partner Help article about automatic replies to learn more.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(24.dp))
                    
                    AutoReplyRow("Check-in", true)
                    AutoReplyRow("Check-out", true)
                    AutoReplyRow("Parking", false)
                    AutoReplyRow("Bed preference", true)
                    AutoReplyRow("Smoking/Non-smoking", false)
                    
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {}, shape = RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) {
                        Text("Create automatic reply", fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), shape = RoundedCornerShape(4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("How do automatic replies work?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("You can set up automatic replies for common questions/requests from guests.", fontSize = 12.sp)
                    Text("\n1. Select the topic your message is about\n2. Choose how you want to reply\n3. Save your preferences", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AutoReplyRow(topic: String, enabled: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if(enabled) Icons.Default.Check else Icons.Default.Close, null, tint = if(enabled) Color(0xFF10B981) else Color(0xFFEF4444), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(topic, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text("Settings", color = Color(0xFF1976D2), fontSize = 12.sp, modifier = Modifier.clickable { })
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
}

@Composable
fun TemplateSchedulerTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Template Scheduler", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Set up automatic templates to give your guests info at the right time.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {}, shape = RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) {
                        Text("Schedule new template", fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("When a guest makes a booking", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1976D2))
                    Text("reservation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Due to extremely high demand, we are currently not accepting bookings without payment. please pay 25% of the total amount to confirm . contact (+915423567944 / +919450872557 manish chaurasiya )", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RemoveCircle, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Text(" Remove from scheduler", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun SecuritySettingsTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Security settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("To improve your account security, specify which email addresses can send messages to guests.", fontSize = 12.sp, color = Color.Gray)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your email addresses", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Add or manage approved email addresses for contacting guests.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = "gangahomestays0@gmail.com", onValueChange = {}, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp), readOnly = true)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray), shape = RoundedCornerShape(4.dp)) { Text("Add email address", color = Color.Gray, fontSize = 12.sp) }
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Text(" Important: Other email addresses will no longer be able to send messages.", fontSize = 10.sp, color = Color.Red, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(thickness = 0.5.dp)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block all email communication", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Prevent any email address from sending messages to guests.", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(checked = true, onCheckedChange = {})
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = {}, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Color(0xFF1976D2))) { Text("Apply to all properties", fontSize = 12.sp, color = Color(0xFF1976D2)) }
                }
            }
        }
    }
}
