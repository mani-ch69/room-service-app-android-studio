package com.example.roomservice.ui.waiter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.roomservice.data.model.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingPerGuestScreen(
    rooms: List<Room>,
    onBackClick: () -> Unit
) {
    val roomTypes = remember(rooms) { rooms.map { it.roomType }.distinct().sorted() }
    var selectedRoomType by remember { mutableStateOf(if (roomTypes.isNotEmpty()) roomTypes[0] else "") }
    var isSidebarExpanded by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pricing per guest", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                    }
                },
                actions = {
                    Text("Was this page helpful?", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                    IconButton(onClick = {}) { Icon(Icons.Default.ThumbUpOffAlt, null, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = {}) { Icon(Icons.Default.ThumbDownOffAlt, null, modifier = Modifier.size(20.dp)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF1F5F9))) {
            // Sidebar with Toggle Logic
            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.TopStart) {
                Row(modifier = Modifier.fillMaxHeight()) {
                    AnimatedVisibility(
                        visible = isSidebarExpanded,
                        enter = expandHorizontally(),
                        exit = shrinkHorizontally()
                    ) {
                        Surface(
                            modifier = Modifier.width(220.dp).fillMaxHeight(),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, Color.LightGray)
                        ) {
                            LazyColumn {
                                items(roomTypes, key = { it }) { type ->
                                    val isSelected = type == selectedRoomType
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedRoomType = type }
                                            .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = type,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        if (isSelected) {
                                            Box(Modifier.align(Alignment.CenterStart).width(4.dp).height(24.dp).background(MaterialTheme.colorScheme.primary))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { isSidebarExpanded = !isSidebarExpanded },
                    modifier = Modifier
                        .offset(x = if (isSidebarExpanded) 205.dp else (-10).dp, y = 12.dp)
                        .size(30.dp)
                        .background(Color.White, CircleShape)
                        .border(0.5.dp, Color.LightGray, CircleShape)
                        .zIndex(10f)
                ) {
                    Icon(
                        imageVector = if (isSidebarExpanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = "Toggle Sidebar",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                }
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                if (selectedRoomType.isNotEmpty()) {
                    val maxGuests = remember(selectedRoomType, rooms) {
                        rooms.filter { it.roomType == selectedRoomType }.maxOfOrNull { it.maxGuests } ?: 2
                    }
                    PricingDetailArea(selectedRoomType, maxGuests)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a room type to manage pricing", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun PricingDetailArea(roomType: String, maxGuests: Int) {
    var baseOccupancy by remember(roomType, maxGuests) { mutableStateOf(maxGuests.toString()) }
    var pricingMode by remember { mutableStateOf("Custom") }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BaseOccupancyDropdown() {
        var occExp by remember { mutableStateOf(false) }
        val occupancies = remember(maxGuests) { (1..maxGuests).map { it.toString() } }
        
        ExposedDropdownMenuBox(
            expanded = occExp,
            onExpandedChange = { occExp = !occExp },
            modifier = Modifier.width(200.dp)
        ) {
            OutlinedTextField(
                value = baseOccupancy,
                onValueChange = {},
                readOnly = true,
                label = { Text("Base occupancy") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(occExp) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(4.dp)
            )
            ExposedDropdownMenu(
                expanded = occExp,
                onDismissRequest = { occExp = false }
            ) {
                occupancies.forEach { occ ->
                    DropdownMenuItem(
                        text = { Text(occ) },
                        onClick = {
                            baseOccupancy = occ
                            occExp = false
                        }
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(text = roomType, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Standard rate", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Base occupancy: $baseOccupancy guests    Max occupancy: $maxGuests guests", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.Gray)
                }

                Spacer(Modifier.height(24.dp))

                Text("What's the base occupancy you want to use for this rate?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Base occupancy is how many guests you want to include for your normal price. The normal price is the baseline for your pricing per guest calculations.", fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
                
                Spacer(Modifier.height(12.dp))
                
                BaseOccupancyDropdown()

                Spacer(Modifier.height(24.dp))

                Text("Set prices per guest", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Adjust your prices based on the number of guests in the group. You can manually set your prices for different occupancies, but we recommend automating the calculation. Do this by setting a fixed price per person, or by setting a percent increase for each additional guest.", fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = pricingMode == "Recommended", onClick = { pricingMode = "Recommended" })
                    Text("Recommended", fontSize = 14.sp)
                    Spacer(Modifier.width(24.dp))
                    RadioButton(selected = pricingMode == "Custom", onClick = { pricingMode = "Custom" })
                    Text("Custom", fontSize = 14.sp)
                }
                Text("Our recommendation is based on data from properties like yours.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 48.dp))

                Spacer(Modifier.height(24.dp))

                // Table Header
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB)).padding(12.dp)) {
                    Text("Occupancy", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Price", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                val currentBase = remember(baseOccupancy) { baseOccupancy.toIntOrNull() ?: 2 }
                
                // Show rows from Max down to 1
                for (i in maxGuests downTo 1) {
                    key(i) {
                        when {
                            i == currentBase -> {
                                PricingRow("$i guests", "Normal price", "", null)
                            }
                            i > currentBase -> {
                                val increase = when(i - currentBase) {
                                    1 -> "15"
                                    2 -> "25"
                                    else -> "35"
                                }
                                PricingRow("$i guests", "Normal price increased by", increase, true)
                            }
                            else -> {
                                val decrease = when(currentBase - i) {
                                    1 -> "10"
                                    else -> "20"
                                }
                                PricingRow("$i guest${if(i>1) "s" else ""}", "Normal price reduced by", decrease, true)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) {
                    Text("Save changes", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PricingRow(occupancy: String, label: String, defaultValue: String, isSwitchOn: Boolean?) {
    var value by remember { mutableStateOf(defaultValue) }
    var isOn by remember { mutableStateOf(isSwitchOn ?: false) }

    Column {
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(occupancy, modifier = Modifier.weight(1f), fontSize = 13.sp)
            
            Row(modifier = Modifier.weight(2f), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, fontSize = 11.sp, color = Color.Gray)
                    if (defaultValue.isNotEmpty()) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            suffix = { Text("%", fontSize = 12.sp) },
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
                
                if (isSwitchOn != null) {
                    Spacer(Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (isOn) "On" else "Off", fontSize = 11.sp, color = Color.Gray)
                        Switch(checked = isOn, onCheckedChange = { isOn = it }, modifier = Modifier.scale(0.8f))
                    }
                }
            }
        }
    }
}
