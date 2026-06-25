package com.example.roomservice.ui.waiter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.CallRepository
import com.example.roomservice.data.OrderRepository
import com.example.roomservice.data.BusinessDetailsRepository
import com.example.roomservice.data.StaffRepository
import com.example.roomservice.data.model.CallRequest
import com.example.roomservice.data.model.CallStatus
import com.example.roomservice.data.model.Order
import com.example.roomservice.data.model.OrderStatus
import com.example.roomservice.data.model.Staff
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    staffId: String,
    staffName: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val securityManager = remember { com.example.roomservice.util.SecurityManager(context) }
    val activity = context as? android.app.Activity
    
    val hotelDetails by BusinessDetailsRepository.details.collectAsState()
    val staffList by StaffRepository.staffList.collectAsState()
    val userData = remember { securityManager.getUserData() }
    val staffRole = userData["role"] ?: "Staff"

    BackHandler {
        activity?.moveTaskToBack(true)
    }

    val calls by CallRepository.calls.collectAsState()
    val allOrdersRaw by OrderRepository.orders.collectAsState()
    
    val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
    val orders = remember(allOrdersRaw) {
        allOrdersRaw.filter { it.timestamp > sevenDaysAgo }
    }

    // 2. ASSIGNED CALLS (For Waiter/Housekeeping)
    val assignedCalls = remember(calls, staffId) {
        calls.filter { it.assignedStaffId == staffId && it.status == CallStatus.ATTENDED }
    }.sortedByDescending { it.timestamp }
    
    var searchQuery by remember { mutableStateOf("") }

    val restaurantOrders = remember(orders, staffRole, searchQuery) {
        if (staffRole == "Restaurant") {
            orders.filter { 
                (it.status == OrderStatus.PENDING || 
                it.status == OrderStatus.PROCESSING || 
                it.status == OrderStatus.READY) &&
                (searchQuery.isEmpty() || it.id.takeLast(6).uppercase().contains(searchQuery.uppercase()))
            }.sortedByDescending { it.timestamp }
        } else {
            emptyList()
        }
    }

    var restaurantFilter by remember { mutableStateOf("All") }
    val filteredRestaurantOrders = remember(restaurantOrders, restaurantFilter) {
        when(restaurantFilter) {
            "Accepted" -> restaurantOrders.filter { it.status == OrderStatus.PROCESSING }
            "Ready" -> restaurantOrders.filter { it.status == OrderStatus.READY }
            else -> restaurantOrders
        }
    }

    val pendingCount = restaurantOrders.count { it.status == OrderStatus.PENDING }
    val acceptedCount = restaurantOrders.count { it.status == OrderStatus.PROCESSING }
    val readyCount = restaurantOrders.count { it.status == OrderStatus.READY }

    var waiterFilter by remember { mutableStateOf("All") }
    val assignedOrders = remember(orders, staffId, staffRole, searchQuery) {
        orders.filter { 
            val isAssigned = it.assignedStaffId == staffId
            val isReadyForWaiter = staffRole == "Waiter" && it.status == OrderStatus.READY
            (isAssigned || isReadyForWaiter) && it.status != OrderStatus.CANCELLED &&
            (searchQuery.isEmpty() || it.id.takeLast(6).uppercase().contains(searchQuery.uppercase()))
        }.sortedByDescending { it.timestamp }
    }

    val filteredAssignedOrders = remember(assignedOrders, waiterFilter) {
        when(waiterFilter) {
            "Pickup" -> assignedOrders.filter { it.status == OrderStatus.READY || it.status == OrderStatus.PICKED_UP || it.status == OrderStatus.ARRIVED }
            "Delivered" -> assignedOrders.filter { it.status == OrderStatus.DELIVERED }
            else -> assignedOrders.filter { it.status != OrderStatus.DELIVERED } // Default to active tasks
        }
    }

    val waiterPickupCount = assignedOrders.count { it.status == OrderStatus.READY || it.status == OrderStatus.PICKED_UP || it.status == OrderStatus.ARRIVED }
    val waiterDeliveredCount = assignedOrders.count { it.status == OrderStatus.DELIVERED }

    val shopItems by com.example.roomservice.data.ShopRepository.shopItems.collectAsState()
    val shopCategories by com.example.roomservice.data.ShopRepository.categories.collectAsState()
    var selectedShopCategory by remember { mutableStateOf("All") }

    val totalTasks = assignedCalls.size + (if(staffRole == "Restaurant") restaurantOrders.size else if(staffRole == "Shop") shopItems.size else assignedOrders.count { it.status != OrderStatus.DELIVERED })

    var selectedBottomTab by remember { mutableStateOf("home") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = when(selectedBottomTab) {
                                "home" -> "Dashboard"
                                "orders" -> "Orders Feed"
                                "messages" -> "Messages"
                                else -> "Staff Menu"
                            }, 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        if (hotelDetails.hotelName.isNotEmpty()) {
                            Text(hotelDetails.hotelName, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Logout", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            if (selectedBottomTab == "shop_inventory") {
                var showAddDialog by remember { mutableStateOf(false) }
                var showAddCatDialog by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                    FloatingActionButton(onClick = { showAddCatDialog = true }, containerColor = Color(0xFF1976D2), contentColor = Color.White, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Category, null)
                    }
                    FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFF1976D2), contentColor = Color.White) {
                        Icon(Icons.Default.Add, null)
                    }
                }

                if (showAddDialog) {
                    com.example.roomservice.ui.waiter.AddItemDialog(
                        currentCategory = if (selectedShopCategory == "All") (shopCategories.firstOrNull()?.name ?: "") else selectedShopCategory,
                        isShop = true,
                        onDismiss = { showAddDialog = false },
                        onConfirm = { n, p, u, c, d, i, s ->
                            com.example.roomservice.data.ShopRepository.addItem(com.example.roomservice.data.model.MenuItem(name = n, price = p, unit = u, category = c, description = d, imageUrl = i, stock = s, itemType = "SHOP"))
                            showAddDialog = false
                        }
                    )
                }
                if (showAddCatDialog) {
                    com.example.roomservice.ui.waiter.AddCategoryDialog(
                        onDismiss = { showAddCatDialog = false },
                        onConfirm = {
                            com.example.roomservice.data.ShopRepository.addCategory(it)
                            showAddCatDialog = false
                        }
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedBottomTab == "home",
                    onClick = { selectedBottomTab = "home" },
                    icon = { Icon(Icons.Default.Dashboard, null) },
                    label = { Text("Dashboard", fontSize = 10.sp) }
                )
                if (staffRole == "Shop") {
                    NavigationBarItem(
                        selected = selectedBottomTab == "shop_inventory",
                        onClick = { selectedBottomTab = "shop_inventory" },
                        icon = { Icon(Icons.Default.Inventory, null) },
                        label = { Text("Inventory", fontSize = 10.sp) }
                    )
                }
                NavigationBarItem(
                    selected = selectedBottomTab == "messages",
                    onClick = { selectedBottomTab = "messages" },
                    icon = { Icon(Icons.Default.Chat, null) },
                    label = { Text("Messages", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == "more",
                    onClick = { selectedBottomTab = "more" },
                    icon = { Icon(Icons.Default.MoreHoriz, null) },
                    label = { Text("More", fontSize = 10.sp) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedBottomTab) {
                "home" -> {
                    StaffDashboardMainContent(
                        staffRole = staffRole,
                        staffId = staffId,
                        staffName = staffName,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        restaurantFilter = restaurantFilter,
                        onRestaurantFilterChange = { restaurantFilter = it },
                        waiterFilter = waiterFilter,
                        onWaiterFilterChange = { waiterFilter = it },
                        restaurantOrders = restaurantOrders,
                        filteredRestaurantOrders = filteredRestaurantOrders,
                        assignedCalls = assignedCalls,
                        assignedOrders = assignedOrders,
                        filteredAssignedOrders = filteredAssignedOrders,
                        pendingCount = pendingCount,
                        acceptedCount = acceptedCount,
                        readyCount = readyCount,
                        waiterPickupCount = waiterPickupCount,
                        waiterDeliveredCount = waiterDeliveredCount,
                        totalTasks = totalTasks,
                        staffList = staffList
                    )
                }
                "shop_inventory" -> {
                    val items = if (selectedShopCategory == "All") shopItems else shopItems.filter { it.category == selectedShopCategory }
                    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
                        ScrollableTabRow(
                            selectedTabIndex = (listOf("All") + shopCategories.map { it.name }).indexOf(selectedShopCategory),
                            edgePadding = 16.dp,
                            containerColor = Color.White,
                            contentColor = Color(0xFF1976D2)
                        ) {
                            (listOf("All") + shopCategories.map { it.name }).forEach { cat ->
                                Tab(selected = selectedShopCategory == cat, onClick = { selectedShopCategory = cat }, text = { Text(cat) })
                            }
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items) { item ->
                                com.example.roomservice.ui.waiter.AdminMenuItemRow(
                                    item = item,
                                    onToggle = { com.example.roomservice.data.ShopRepository.toggleAvailability(item.id, !item.isAvailable) },
                                    onDelete = { com.example.roomservice.data.ShopRepository.deleteItem(item.id) }
                                )
                            }
                        }
                    }
                }
                "messages" -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chat Interface Coming Soon", fontWeight = FontWeight.Bold)
                    }
                }
                "more" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F3F4))) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Staff Settings", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    ListItem(
                                        headlineContent = { Text("Profile Information") },
                                        leadingContent = { Icon(Icons.Default.Person, null) },
                                        modifier = Modifier.clickable { }
                                    )
                                    ListItem(
                                        headlineContent = { Text("Logout Account", color = Color.Red) },
                                        leadingContent = { Icon(Icons.Default.Logout, null, tint = Color.Red) },
                                        modifier = Modifier.clickable { onLogout() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffDashboardMainContent(
    staffRole: String,
    staffId: String,
    staffName: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    restaurantFilter: String,
    onRestaurantFilterChange: (String) -> Unit,
    waiterFilter: String,
    onWaiterFilterChange: (String) -> Unit,
    restaurantOrders: List<Order>,
    filteredRestaurantOrders: List<Order>,
    assignedCalls: List<CallRequest>,
    assignedOrders: List<Order>,
    filteredAssignedOrders: List<Order>,
    pendingCount: Int,
    acceptedCount: Int,
    readyCount: Int,
    waiterPickupCount: Int,
    waiterDeliveredCount: Int,
    totalTasks: Int,
    staffList: List<Staff>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = Color(0xFF1976D2).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(staffName.take(1).uppercase(), color = Color(0xFF1976D2), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Welcome, $staffName", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("$staffRole • RS-$staffId", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }

        if (staffRole == "Restaurant" || staffRole == "Waiter") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search Order Number...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if(searchQuery.isNotEmpty()) IconButton(onClick = { onSearchChange("") }) { Icon(Icons.Default.Close, null) } },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF1976D2)
                ),
                singleLine = true
            )
        }

        if (staffRole == "Restaurant" && restaurantOrders.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = when(restaurantFilter) {
                    "Accepted" -> 1
                    "Ready" -> 2
                    else -> 0
                },
                edgePadding = 16.dp,
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2),
                divider = {}
            ) {
                Tab(
                    selected = restaurantFilter == "All",
                    onClick = { onRestaurantFilterChange("All") },
                    text = { Text("New ($pendingCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = restaurantFilter == "Accepted",
                    onClick = { onRestaurantFilterChange("Accepted") },
                    text = { Text("Accepted ($acceptedCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = restaurantFilter == "Ready",
                    onClick = { onRestaurantFilterChange("Ready") },
                    text = { Text("Ready ($readyCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
        }

        if (staffRole == "Waiter" && assignedOrders.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = when(waiterFilter) {
                    "Pickup" -> 1
                    "Delivered" -> 2
                    else -> 0
                },
                edgePadding = 16.dp,
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2),
                divider = {}
            ) {
                Tab(
                    selected = waiterFilter == "All",
                    onClick = { onWaiterFilterChange("All") },
                    text = { Text("Active (${assignedOrders.count { it.status != OrderStatus.DELIVERED }})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = waiterFilter == "Pickup",
                    onClick = { onWaiterFilterChange("Pickup") },
                    text = { Text("Pickup ($waiterPickupCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = waiterFilter == "Delivered",
                    onClick = { onWaiterFilterChange("Delivered") },
                    text = { Text("Delivered ($waiterDeliveredCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
        }

        Text(
            "TASKS & REQUESTS (${if(staffRole == "Restaurant") restaurantOrders.size else if(staffRole == "Waiter") filteredAssignedOrders.size else totalTasks})",
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )

        if (totalTasks == 0 && restaurantOrders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tasks assigned to you.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. INCOMING RESTAURANT ORDERS
                items(if (staffRole == "Restaurant") filteredRestaurantOrders else emptyList()) { order ->
                    val btnText = when(order.status) {
                        OrderStatus.PENDING -> "ACCEPT"
                        OrderStatus.PROCESSING -> "MARK READY"
                        OrderStatus.READY -> "READY"
                        else -> "DONE"
                    }
                    val label = when(order.status) {
                        OrderStatus.PENDING -> "NEW ORDER"
                        OrderStatus.PROCESSING -> "PREPARING FOOD"
                        OrderStatus.READY -> "WAITING FOR PICKUP"
                        else -> "ORDER"
                    }
                    val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    val dateTimeStr = timeFormat.format(Date(order.timestamp))
                    
                    StaffTaskCard(
                        roomNumber = order.roomNumber,
                        taskId = order.id,
                        label = label,
                        secondaryLabel = "Placed: $dateTimeStr",
                        icon = Icons.Default.Restaurant,
                        color = when(order.status) {
                            OrderStatus.PENDING -> Color(0xFFE91E63)
                            OrderStatus.PROCESSING -> Color(0xFFFFA000)
                            OrderStatus.READY -> Color(0xFF2E7D32)
                            else -> Color.Gray
                        },
                        buttonText = btnText,
                        buttonColor = if (order.status == OrderStatus.READY) Color.Gray else Color(0xFF2E7D32),
                        orderItems = order.items,
                        deliveryPin = if (order.status == OrderStatus.READY) order.deliveryPin else null,
                        isCompact = true
                    ) {
                        when(order.status) {
                            OrderStatus.PENDING -> OrderRepository.acceptOrderAndStartProcessing(order.id)
                            OrderStatus.PROCESSING -> OrderRepository.markOrderAsReadyAndAssignWaiter(order.id, staffList)
                            else -> {}
                        }
                    }
                }

                // 2. ASSIGNED CALLS
                if (staffRole != "Restaurant") {
                    items(assignedCalls) { call ->
                        val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        val dateTimeStr = timeFormat.format(Date(call.timestamp))
                        StaffTaskCard(
                            roomNumber = call.roomNumber,
                            taskId = call.id,
                            label = "Service Call",
                            secondaryLabel = "Requested: $dateTimeStr",
                            icon = Icons.Default.NotificationsActive,
                            color = Color.Red,
                            buttonText = "DONE",
                            isCompact = true
                        ) { CallRepository.attendCall(call.id) }
                    }
                }
                
                // 3. ASSIGNED ORDERS
                if (staffRole == "Waiter") {
                    items(filteredAssignedOrders) { order ->
                        val btnText = when {
                            order.status == OrderStatus.READY -> "PICKUP"
                            order.status == OrderStatus.PICKED_UP -> "ARRIVED"
                            order.status == OrderStatus.ARRIVED -> "DELIVER"
                            order.status == OrderStatus.DELIVERED -> "DONE"
                            else -> "DONE"
                        }
                        var showPinDialog by remember { mutableStateOf(false) }
                        val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        val dateTimeStr = timeFormat.format(Date(order.timestamp))

                        StaffTaskCard(
                            roomNumber = order.roomNumber,
                            taskId = order.id,
                            label = when(order.status) {
                                OrderStatus.READY -> "READY FOR PICKUP"
                                OrderStatus.PICKED_UP -> "OUT FOR DELIVERY"
                                OrderStatus.ARRIVED -> "ARRIVED AT ROOM"
                                OrderStatus.DELIVERED -> "DELIVERED"
                                else -> "ORDER"
                            },
                            secondaryLabel = "Placed: $dateTimeStr",
                            icon = Icons.Default.Restaurant,
                            color = when(order.status) {
                                OrderStatus.READY -> Color(0xFF00ACC1)
                                OrderStatus.PICKED_UP -> Color(0xFFFB8C00)
                                OrderStatus.ARRIVED -> Color(0xFF673AB7)
                                OrderStatus.DELIVERED -> Color(0xFF2E7D32)
                                else -> Color.Gray
                            },
                            buttonText = btnText,
                            orderItems = order.items,
                            deliveryPin = if (order.status == OrderStatus.READY) order.deliveryPin else null,
                            isCompact = true
                        ) {
                            if (order.status == OrderStatus.READY || order.status == OrderStatus.ARRIVED) {
                                showPinDialog = true
                            } else if (order.status == OrderStatus.PICKED_UP) {
                                OrderRepository.updateOrderStatus(order.id, OrderStatus.ARRIVED)
                            } else {
                                OrderRepository.updateOrderStatus(order.id, OrderStatus.DELIVERED)
                            }
                        }

                        if (showPinDialog) {
                            WaiterPinVerifyDialog(
                                order = order,
                                isPickup = order.status == OrderStatus.READY,
                                onDismiss = { showPinDialog = false },
                                onVerified = {
                                    if (order.status == OrderStatus.READY) {
                                        OrderRepository.assignStaffToOrder(order.id, staffId, staffName)
                                        OrderRepository.updateOrderStatus(order.id, OrderStatus.PICKED_UP)
                                    } else {
                                        OrderRepository.updateOrderStatus(order.id, OrderStatus.DELIVERED)
                                    }
                                    showPinDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaiterPinVerifyDialog(
    order: Order,
    isPickup: Boolean,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPickup) "Pickup Confirmation" else "Delivery Confirmation") },
        text = {
            Column {
                Text(if (isPickup) "Ask Restaurant for 4-digit PIN" else "Ask Guest for 4-digit PIN")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = false },
                    label = { Text("4-Digit PIN") },
                    isError = error,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Text("Invalid PIN", color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (pin == order.deliveryPin) onVerified()
                else error = true
            }) { Text("VERIFY") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun StaffTaskCard(
    roomNumber: String,
    taskId: String? = null,
    label: String,
    secondaryLabel: String? = null,
    icon: ImageVector,
    color: Color,
    buttonText: String = "DONE",
    buttonColor: Color = Color(0xFF2E7D32),
    orderItems: List<com.example.roomservice.data.model.OrderItem> = emptyList(),
    deliveryPin: String? = null,
    isCompact: Boolean = false,
    onDecline: (() -> Unit)? = null,
    onComplete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(if (isCompact) 32.dp else 40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.1f)
                    ) {
                        Icon(icon, null, modifier = Modifier.padding(if (isCompact) 6.dp else 8.dp), tint = color)
                    }
                    Spacer(Modifier.width(if (isCompact) 10.dp else 16.dp))
                    Column {
                        Text("Room $roomNumber", fontSize = if (isCompact) 12.sp else 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        if (taskId != null) {
                            Text(
                                text = "Order: #${taskId.takeLast(6).uppercase()}", 
                                fontSize = if (isCompact) 15.sp else 18.sp,
                                fontWeight = FontWeight.ExtraBold, 
                                color = Color(0xFF1976D2)
                            )
                        }
                        Text(label, fontSize = if (isCompact) 10.sp else 12.sp, color = color, fontWeight = FontWeight.Bold)
                        if (secondaryLabel != null) {
                            Text(secondaryLabel, fontSize = if (isCompact) 9.sp else 11.sp, color = Color.Gray)
                        }
                    }
                }
                
                if (orderItems.isEmpty() && onDecline == null) {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 12.dp),
                        modifier = Modifier.height(if (isCompact) 32.dp else 40.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(if (isCompact) 14.dp else 16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(buttonText, fontSize = if (isCompact) 10.sp else 12.sp)
                    }
                }
            }

            if (deliveryPin != null) {
                Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE3F2FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(if (isCompact) 8.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("PIN: ", fontSize = if (isCompact) 12.sp else 14.sp, fontWeight = FontWeight.Bold)
                        Text(deliveryPin, fontSize = if (isCompact) 20.sp else 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF1976D2), letterSpacing = 1.sp)
                    }
                }
            }
            
            if (orderItems.isNotEmpty()) {
                Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(Modifier.height(if (isCompact) 4.dp else 8.dp))
                
                orderItems.forEach { item ->
                    Text(
                        text = "• ${item.quantity}x ${item.name}",
                        fontSize = if (isCompact) 12.sp else 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = if (isCompact) 1.dp else 2.dp)
                    )
                }
                
                Spacer(Modifier.height(if (isCompact) 12.dp else 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onDecline != null) {
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f).height(if (isCompact) 36.dp else 48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("DECLINE", fontSize = if (isCompact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f).height(if (isCompact) 36.dp else 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(if (isCompact) 14.dp else 16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(buttonText, fontSize = if (isCompact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (onDecline != null) {
                Spacer(Modifier.height(if (isCompact) 12.dp else 16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f).height(if (isCompact) 36.dp else 48.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("DECLINE", fontSize = if (isCompact) 11.sp else 12.sp) }
                    Button(onClick = onComplete, modifier = Modifier.weight(1f).height(if (isCompact) 36.dp else 48.dp), colors = ButtonDefaults.buttonColors(containerColor = buttonColor)) { Text(buttonText, fontSize = if (isCompact) 11.sp else 12.sp) }
                }
            }
        }
    }
}
