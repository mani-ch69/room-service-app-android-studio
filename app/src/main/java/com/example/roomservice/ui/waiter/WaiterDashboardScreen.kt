package com.example.roomservice.ui.waiter

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomservice.data.model.CallRequest
import com.example.roomservice.data.model.Order
import com.example.roomservice.data.model.OrderStatus

import com.example.roomservice.data.model.Staff
import com.example.roomservice.ui.util.zoomClick
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaiterDashboardScreen(
    onChatClick: (String) -> Unit = {},
    viewModel: WaiterDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.background(Color.White)) {
                    TopAppBar(
                        title = { Text("Staff Control Center", fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = { viewModel.refreshData() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Black)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = Color.Black
                        )
                    )
                    
                    // Summary Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SummaryItem(label = "Calls", count = uiState.activeCalls.size, color = Color(0xFFC62828))
                        SummaryItem(label = "Orders", count = uiState.activeOrders.size, color = Color(0xFFFFA000))
                        SummaryItem(label = "Rooms", count = uiState.roomStatuses.size, color = Color.Black)
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = Color.Gray
                        ) {
                            Text("Rooms", modifier = Modifier.padding(16.dp))
                        }
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = Color.Gray
                        ) {
                            BadgedBox(badge = { if(uiState.activeCalls.isNotEmpty()) Badge { Text("${uiState.activeCalls.size}") } }) {
                                Text("Calls", modifier = Modifier.padding(16.dp))
                            }
                        }
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = Color.Gray
                        ) {
                            BadgedBox(badge = { if(uiState.activeOrders.isNotEmpty()) Badge { Text("${uiState.activeOrders.size}") } }) {
                                Text("Orders", modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            when (selectedTab) {
                0 -> RoomStatusGrid(
                    statuses = uiState.roomStatuses,
                    staffList = uiState.staffList,
                    onChat = onChatClick,
                    onAttend = { viewModel.markAsAttended(it) },
                    onAssignStaffToCall = { id, staff -> viewModel.assignStaffToCall(id, staff) },
                    onAssignStaffToOrder = { id, staff -> viewModel.assignStaffToOrder(id, staff) }
                )
                1 -> CallsList(uiState.activeCalls) { viewModel.markAsAttended(it) }
                2 -> OrdersList(
                    orders = uiState.activeOrders,
                    staffList = uiState.staffList,
                    onStatusUpdate = { id, status -> viewModel.updateOrderStatus(id, status) },
                    onAssignStaff = { id, staff -> viewModel.assignStaffToOrder(id, staff) }
                )
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun RoomStatusGrid(
    statuses: List<RoomLiveStatus>,
    staffList: List<Staff>,
    onChat: (String) -> Unit,
    onAttend: (String) -> Unit,
    onAssignStaffToCall: (String, Staff) -> Unit,
    onAssignStaffToOrder: (String, Staff) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(statuses) { status ->
            RoomStatusCard(
                status = status,
                staffList = staffList,
                onAttend = { status.activeCall?.let { onAttend(it.id) } },
                onChat = { onChat(status.room.roomNumber) },
                onAssignStaffToCall = { staff -> status.activeCall?.let { onAssignStaffToCall(it.id, staff) } },
                onAssignStaffToOrder = { staff -> status.activeOrder?.let { onAssignStaffToOrder(it.id, staff) } },
                onReceiveCall = { onAttend(it) }
            )
        }
    }
}

@Composable
fun CallsList(calls: List<CallRequest>, onAttend: (String) -> Unit) {
    if (calls.isEmpty()) {
        EmptyState(Icons.Default.NotificationsNone, "No active calls from guests")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(calls) { call ->
                CallItem(call = call, onAttendClick = { onAttend(call.id) })
            }
        }
    }
}

@Composable
fun CallItem(call: CallRequest, onAttendClick: () -> Unit) {
    val waitTime = (System.currentTimeMillis() - call.timestamp) / 1000 / 60
    Card(
        modifier = Modifier.fillMaxWidth().zoomClick(onClick = onAttendClick),
        colors = CardDefaults.cardColors(containerColor = if (waitTime > 2) Color(0xFFFFEBEE) else Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                    if (waitTime > 2) {
                        Surface(
                            modifier = Modifier.size(10.dp).align(Alignment.TopEnd),
                            shape = CircleShape,
                            color = Color.Red
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Room ${call.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(text = "Waiting for $waitTime min", color = if (waitTime > 2) Color.Red else Color.Gray, fontSize = 14.sp)
                }
            }
            Button(
                onClick = onAttendClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ATTEND")
            }
        }
    }
}
