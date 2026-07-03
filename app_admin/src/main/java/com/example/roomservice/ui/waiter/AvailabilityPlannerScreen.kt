package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.RoomRepository

@Composable
fun AvailabilityPlannerScreen() {
    val rooms by RoomRepository.rooms.collectAsState()
    val roomTypes = remember(rooms) { rooms.map { it.roomType }.distinct() }
    
    var selectedAdvanceWindow by remember { mutableStateOf("365 days") }
    var updateExistingPrices by remember { mutableStateOf(true) }
    val roomPrices = remember { mutableStateMapOf<String, String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Availability planner",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Enable this tool to continuously open up new dates and stay visible in the guest search. Learn more about this tool",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Progress Indicator
            item {
                PlannerProgressIndicator(currentStep = 1)
            }

            // Main Config Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("How far in advance can guests book?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Every day, your availability will extend to this length", fontSize = 11.sp, color = Color.Gray)
                        
                        Spacer(Modifier.height(16.dp))
                        
                        PlannerDropdown(selectedAdvanceWindow) { selectedAdvanceWindow = it }
                    }
                }
            }

            // Pricing Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("How much would you like to charge?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("This will be the price of the new available dates. If you have other rate plans, they'll be calculated based on your price settings in the rate plan.", fontSize = 11.sp, color = Color.Gray)
                        
                        Spacer(Modifier.height(24.dp))
                        
                        roomTypes.forEach { type ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(type, fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = roomPrices[type] ?: "",
                                    onValueChange = { roomPrices[type] = it },
                                    modifier = Modifier.width(120.dp).height(48.dp),
                                    suffix = { Text("₹", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(4.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                            Switch(
                                checked = updateExistingPrices, 
                                onCheckedChange = { updateExistingPrices = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1976D2))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Also update the existing prices in the calendar", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            // Info Box
            item {
                Surface(
                    color = Color(0xFFFFF7E6),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD591)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                "Today, this tool will update your availability and price for the next $selectedAdvanceWindow. Once another date enters the period, this tool will open up all the rooms for that date and update the price, unless a room is booked.",
                                fontSize = 12.sp, color = Color(0xFF9A3412), lineHeight = 18.sp
                            )
                            Text("Learn more about how it works", color = Color(0xFF1976D2), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp).clickable { })
                        }
                    }
                }
            }

            // Side Info Box 1
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Column {
                            Text("Can I change availability for single days?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("You can always close dates in your calendar. This tool won't reopen them.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC))
                ) {
                    Text("Continue to review", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PlannerProgressIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProgressStep(1, "Select details", currentStep >= 1)
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = Color.LightGray)
        ProgressStep(2, "Review", currentStep >= 2)
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = Color.LightGray)
        ProgressStep(3, "Completed", currentStep >= 3)
    }
}

@Composable
fun ProgressStep(num: Int, label: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = if(isActive) Color(0xFF1976D2) else Color.White,
            shape = CircleShape,
            border = if(!isActive) androidx.compose.foundation.BorderStroke(1.dp, Color.Gray) else null,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$num", color = if(isActive) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, color = if(isActive) Color.Black else Color.Gray, fontWeight = if(isActive) FontWeight.Bold else FontWeight.Normal)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerDropdown(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("30 days", "60 days", "90 days", "180 days", "365 days", "16 months")
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFCBD5E1))
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
