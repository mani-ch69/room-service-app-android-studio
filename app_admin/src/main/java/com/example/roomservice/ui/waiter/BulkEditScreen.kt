package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.model.Room
import com.example.roomservice.ui.common.CommonDateRangePicker
import com.example.roomservice.ui.common.DateDisplayBox
import com.example.roomservice.ui.common.DateRangeUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BulkEditScreen(
    rooms: List<Room>,
    onBack: () -> Unit,
    onSave: (Long, Long, List<String>, Int, Double, Boolean) -> Unit
) {
    var fromDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var toDate by remember { mutableLongStateOf(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L) }
    val selectedRoomTypes = remember { mutableStateListOf<String>() }
    
    var roomsToSell by remember { mutableIntStateOf(1) }
    var price by remember { mutableStateOf("") }
    var isOpenStatus by remember { mutableStateOf(true) }
    var showRangePicker by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val roomTypes = remember(rooms) { rooms.map { it.roomType }.distinct() }

    // Validation: Find the minimum totalUnits among selected room types
    val maxAllowedQuantity = remember(selectedRoomTypes, rooms) {
        if (selectedRoomTypes.isEmpty()) 100 
        else {
            selectedRoomTypes.map { type ->
                rooms.find { it.roomType == type }?.totalUnits ?: 0
            }.minOrNull() ?: 1
        }
    }

    LaunchedEffect(maxAllowedQuantity) {
        if (roomsToSell > maxAllowedQuantity) {
            roomsToSell = maxAllowedQuantity
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Edit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
                Button(
                    onClick = { 
                        onSave(fromDate, toDate, selectedRoomTypes.toList(), roomsToSell, price.toDoubleOrNull() ?: 0.0, isOpenStatus) 
                    },
                    modifier = Modifier.padding(16.dp).fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    enabled = selectedRoomTypes.isNotEmpty()
                ) {
                    Text("Apply Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F5F9)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. DATE SELECTION
            item {
                SectionCard("Select Date Range") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateDisplayBox("From", sdf.format(Date(fromDate)), { showRangePicker = true }, Modifier.weight(1f))
                        DateDisplayBox("To", sdf.format(Date(toDate)), { showRangePicker = true }, Modifier.weight(1f))
                    }
                }
            }

            // 2. ROOM TYPE SELECTION
            item {
                SectionCard("Select Room Types") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        roomTypes.forEach { type ->
                            val isSelected = selectedRoomTypes.contains(type)
                            val totalUnits = rooms.find { it.roomType == type }?.totalUnits ?: 0
                            
                            Surface(
                                onClick = { if(isSelected) selectedRoomTypes.remove(type) else selectedRoomTypes.add(type) },
                                shape = RoundedCornerShape(8.dp),
                                color = if(isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if(isSelected) MaterialTheme.colorScheme.primary else Color.LightGray)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(type, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Max Capacity: $totalUnits rooms", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Checkbox(checked = isSelected, onCheckedChange = null)
                                }
                            }
                        }
                    }
                }
            }

            // 3. INVENTORY & PRICE
            item {
                SectionCard("Update Inventory & Rates") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("Rooms to sell", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Cannot exceed the number of rooms created in Property Detail ($maxAllowedQuantity for selected types)", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            SimpleCounter(
                                value = roomsToSell,
                                maxValue = maxAllowedQuantity,
                                onValueChange = { roomsToSell = it }
                            )
                        }

                        OutlinedTextField(
                            value = price,
                            onValueChange = { if(it.isEmpty() || it.toDoubleOrNull() != null) price = it },
                            label = { Text("Price per night (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Room Status:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.width(16.dp))
                            FilterChip(
                                selected = isOpenStatus,
                                onClick = { isOpenStatus = true },
                                label = { Text("Open") }
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = !isOpenStatus,
                                onClick = { isOpenStatus = false },
                                label = { Text("Closed") }
                            )
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    if (showRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = fromDate,
            initialSelectedEndDateMillis = toDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return DateRangeUtils.isSelectableFromToday(utcTimeMillis)
                }
            }
        )
        CommonDateRangePicker(
            state = dateRangePickerState,
            onDismiss = { showRangePicker = false },
            onConfirm = { start, end ->
                start?.let { fromDate = it }
                end?.let { toDate = it }
            }
        )
    }
}
