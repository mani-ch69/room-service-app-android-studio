package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun OpenCloseRoomsScreen(
    rooms: List<Room>,
    onBack: () -> Unit,
    onSave: (Long, Long, List<String>, List<String>, Boolean) -> Unit
) {
    var fromDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var toDate by remember { mutableLongStateOf(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L) }
    val daysOfWeek = remember { mutableStateListOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }
    val selectedRoomTypes = remember { mutableStateListOf<String>() }
    var isOpenStatus by remember { mutableStateOf(true) }
    var showRangePicker by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val roomTypes = remember(rooms) { rooms.map { it.roomType }.distinct() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open/close rooms", fontWeight = FontWeight.Bold) },
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
                    onClick = { onSave(fromDate, toDate, daysOfWeek, selectedRoomTypes, isOpenStatus) },
                    modifier = Modifier.padding(16.dp).fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Save changes", fontWeight = FontWeight.Bold)
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
                SectionCard("Which dates do you want to make changes to?") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DateDisplayBox("From Date:", sdf.format(Date(fromDate)), { showRangePicker = true }, Modifier.weight(1f))
                            DateDisplayBox("To Date:", sdf.format(Date(toDate)), { showRangePicker = true }, Modifier.weight(1f))
                        }
                        
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = daysOfWeek.contains(day),
                                        onCheckedChange = { if(it) daysOfWeek.add(day) else daysOfWeek.remove(day) }
                                    )
                                    Text(day, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. ROOM TYPE SELECTION
            item {
                SectionCard("Which room types do you want to open or close?") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (roomTypes.isEmpty()) {
                            Text("No room types found in Property Detail.", color = Color.Gray, fontSize = 13.sp)
                        }
                        roomTypes.forEach { type ->
                            Column {
                                Text(type, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedRoomTypes.contains(type),
                                        onCheckedChange = { if(it) selectedRoomTypes.add(type) else selectedRoomTypes.remove(type) }
                                    )
                                    Text("Entire room", fontSize = 13.sp)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }

            // 3. STATUS SELECTION
            item {
                SectionCard("Set the selected rooms and rates as:") {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isOpenStatus, onClick = { isOpenStatus = true })
                            Text("Open", fontSize = 14.sp)
                            Spacer(Modifier.width(24.dp))
                            RadioButton(selected = !isOpenStatus, onClick = { isOpenStatus = false })
                            Text("Closed", fontSize = 14.sp)
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

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}
