package com.example.roomservice.ui.waiter

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import coil.compose.AsyncImage
import com.example.roomservice.data.model.Order
import com.example.roomservice.data.model.OrderStatus
import com.example.roomservice.data.model.Staff
import com.example.roomservice.data.model.CallRequest
import com.example.roomservice.data.model.Notification
import com.example.roomservice.data.model.NotificationType
import com.example.roomservice.ui.util.zoomClick
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationBell(
    unreadCount: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = Color.Gray,
                        modifier = Modifier.size(6.dp)
                    )
                }
            }
        ) {
            Icon(
                imageVector = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = if (unreadCount > 0) Color(0xFFFFC107) else Color.Gray
            )
        }
    }
}

@Composable
fun NotificationListDialog(
    notifications: List<Notification>,
    onDismiss: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications", fontWeight = FontWeight.ExtraBold)
                if (notifications.any { !it.isRead }) {
                    TextButton(onClick = onMarkAllAsRead) {
                        Text("Mark all read", fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("No notifications yet", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onMarkAsRead = { onMarkAsRead(notification.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (notifications.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("CLEAR ALL", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
fun NotificationItem(
    notification: Notification,
    onMarkAsRead: () -> Unit
) {
    val (icon, color) = when (notification.type) {
        NotificationType.CALL_STAFF -> Icons.Default.NotificationsActive to Color.Red
        NotificationType.NEW_ORDER -> Icons.Default.ShoppingCart to Color(0xFF1976D2)
        NotificationType.ORDER_ACCEPTED -> Icons.Default.TaskAlt to Color(0xFF2196F3)
        NotificationType.ORDER_READY -> Icons.Default.Restaurant to Color(0xFFFFA000)
        NotificationType.ORDER_PICKED_UP -> Icons.Default.DeliveryDining to Color(0xFFFB8C00)
        NotificationType.ORDER_DELIVERED -> Icons.Default.CheckCircle to Color(0xFF2E7D32)
        NotificationType.NEW_MESSAGE -> Icons.Default.Chat to Color(0xFF673AB7)
        else -> Icons.Default.Info to Color.Gray
    }

    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(notification.timestamp))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { if (!notification.isRead) onMarkAsRead() },
        color = if (notification.isRead) Color.Transparent else color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = if (notification.isRead) null else BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(notification.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(time, fontSize = 10.sp, color = Color.Gray)
                }
                Text(notification.message, fontSize = 12.sp, color = if (notification.isRead) Color.Gray else Color.Black)
            }
            if (!notification.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            }
        }
    }
}

@Composable
fun ViewModeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun RoomStatusCard(
    status: RoomLiveStatus,
    isCompact: Boolean = false,
    onAttend: () -> Unit,
    onChat: () -> Unit,
    onAcceptOrder: (String) -> Unit = {},
    onCancelOrder: (String) -> Unit = {},
    onCompleteOrder: (String) -> Unit = {},
    staffList: List<Staff> = emptyList(),
    onAssignStaffToCall: (Staff) -> Unit = {},
    onAssignStaffToOrder: (Staff) -> Unit = {},
    onReceiveCall: (String) -> Unit = {}
) {
    val isActive = status.activeCall != null || status.activeOrder != null
    var showAssignmentDialog by remember { mutableStateOf(false) }
    
    // Timer Logic for Active Call
    var timeLeft by remember(status.activeCall?.id) { mutableIntStateOf(60) }
    var isTimerStopped by remember { mutableStateOf(false) }

    LaunchedEffect(status.activeCall, isTimerStopped) {
        if (status.activeCall != null && !isTimerStopped && timeLeft > 0) {
            while(timeLeft > 0) {
                kotlinx.coroutines.delay(1000)
                timeLeft--
            }
        }
    }

    // Colors aligned with Home Dashboard
    val backgroundColor = Color.White

    val borderColor = if (isActive) {
        if (status.activeCall != null) Color.Red else Color(0xFF2E7D32)
    } else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCompact) Modifier.height(110.dp) else Modifier)
            .zoomClick(onClick = { 
                if (isActive) showAssignmentDialog = true
                else onChat()
            }),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (isActive) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isActive) BorderStroke(1.5.dp, borderColor) else null
    ) {
        if (isCompact) {
            // MINI COMPACT VIEW (Styled like Dashboard Mini Items)
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val ledColor = if (status.room.isAvailable) Color(0xFF00E676) else Color.Red
                    
                    Box(contentAlignment = Alignment.Center) {
                        Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = ledColor) {}
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    Text(text = "R-${status.room.roomNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    
                    if (status.activeCall != null) {
                        Icon(Icons.Default.NotificationsActive, null, Modifier.size(18.dp), tint = Color.Red)
                    } else if (status.activeOrder != null) {
                        Icon(Icons.Default.Fastfood, null, Modifier.size(18.dp), tint = Color(0xFF2E7D32))
                    }
                }
            }
        } else {
            // NORMAL DETAILED VIEW
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val imageToShow = if (status.room.imageUrl.isNotEmpty()) status.room.imageUrl 
                                     else "https://img.freepik.com/free-vector/interior-hotel-room-with-bed-window-sketch_107791-3048.jpg"
                    
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(imageToShow)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    if (isActive) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(36.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                imageVector = if (status.activeCall != null) Icons.Default.NotificationsActive else Icons.Default.Fastfood,
                                contentDescription = null, tint = borderColor, modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = "Room ${status.room.roomNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            if (status.activeCall != null && !isTimerStopped) {
                                Text(
                                    text = "Timer: ${timeLeft}s", 
                                    color = if (timeLeft < 10) Color.Red else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (isActive) {
                            Badge(containerColor = borderColor, contentColor = Color.White) { 
                                Text(if (status.activeCall != null) "CALL" else "ORDER", modifier = Modifier.padding(horizontal = 4.dp)) 
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (status.activeOrder != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                status.activeOrder.items.forEach { item ->
                                    Text("• ${item.quantity}x ${item.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(Modifier.padding(vertical = 6.dp), thickness = 0.5.dp)
                                Text("Total: ₹${status.activeOrder.totalAmount}", fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            if (status.activeCall != null) {
                                if (!isTimerStopped) {
                                    isTimerStopped = true
                                    onReceiveCall(status.activeCall.id)
                                } else {
                                    showAssignmentDialog = true
                                }
                            } else if (status.activeOrder != null) {
                                showAssignmentDialog = true 
                            } else {
                                onChat()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isActive) borderColor else Color(0xFF1976D2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val btnText = when {
                            status.activeCall != null -> if (!isTimerStopped) "RECEIVE CALL" else "ASSIGN STAFF"
                            status.activeOrder != null -> "PROCESS ORDER"
                            else -> "OPEN CHAT"
                        }
                        Text(btnText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAssignmentDialog) {
        val filteredStaff = when {
            status.activeCall != null -> staffList.filter { it.role == "Housekeeping" }
            status.activeOrder != null -> staffList.filter { it.role == "Waiter" || it.role == "Restaurant" }
            else -> staffList
        }

        StaffAssignmentDialog(
            staffList = filteredStaff,
            onDismiss = { showAssignmentDialog = false },
            onAssign = { staff ->
                if (status.activeCall != null) onAssignStaffToCall(staff)
                else if (status.activeOrder != null) onAssignStaffToOrder(staff)
                showAssignmentDialog = false
            }
        )
    }
}

@Composable
fun StaffAssignmentDialog(
    staffList: List<Staff>,
    onDismiss: () -> Unit,
    onAssign: (Staff) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Staff Member", fontWeight = FontWeight.Bold) },
        text = {
            if (staffList.isEmpty()) {
                Text("No staff members added yet. Please add staff from menu.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(staffList) { staff ->
                        ListItem(
                            headlineContent = { Text(staff.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { 
                                Text(
                                    text = "ID: RS-${staff.code} • ${staff.role}",
                                    color = if (staff.isAvailable) Color(0xFF2E7D32) else Color.Gray,
                                    fontWeight = if (staff.isAvailable) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            leadingContent = { 
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = if (staff.isAvailable) Color(0xFFE8F5E9) else Color(0xFFF1F5F9)
                                ) {
                                    Icon(
                                        Icons.Default.Person, 
                                        null, 
                                        modifier = Modifier.padding(8.dp),
                                        tint = if (staff.isAvailable) Color(0xFF2E7D32) else Color.Gray
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onAssign(staff) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun StatusBadge(status: OrderStatus) {
    val (label, color) = when (status) {
        OrderStatus.PENDING -> "Pending" to Color.Gray
        OrderStatus.ACCEPTED -> "Restaurant Accepted" to Color(0xFF2196F3)
        OrderStatus.NOT_ACCEPTED -> "Declined" to Color.Red
        OrderStatus.PROCESSING -> "Preparing Food" to Color(0xFFFFA000)
        OrderStatus.READY -> "Order is Ready" to Color(0xFF00ACC1)
        OrderStatus.PICKED_UP -> "Order Pick-up" to Color(0xFFFB8C00)
        OrderStatus.ARRIVED -> "Arrived" to Color(0xFF673AB7)
        OrderStatus.DELIVERED -> "Delivered" to Color(0xFF2E7D32)
        OrderStatus.CANCELLED -> "Cancelled" to Color.Red
        else -> status.name to Color.Gray
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OrdersList(
    orders: List<Order>, 
    staffList: List<Staff> = emptyList(),
    onStatusUpdate: (String, OrderStatus) -> Unit,
    onAssignStaff: (String, Staff) -> Unit = { _, _ -> }
) {
    if (orders.isEmpty()) {
        EmptyState(Icons.Default.Restaurant, "No active orders to process")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(orders) { order ->
                OrderItemCard(
                    order = order, 
                    staffList = staffList,
                    onStatusUpdate = onStatusUpdate,
                    onAssignStaff = onAssignStaff
                )
            }
        }
    }
}

@Composable
fun OrderItemCard(
    order: Order, 
    staffList: List<Staff> = emptyList(),
    onStatusUpdate: (String, OrderStatus) -> Unit,
    onAssignStaff: (String, Staff) -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var nextStatusAfterAssign by remember { mutableStateOf<OrderStatus?>(null) }
    
    val dateTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.timestamp))

    val isPending = order.status == OrderStatus.PENDING

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = Color(0xFFF1F5F9)) {
                        Icon(Icons.Default.Restaurant, null, modifier = Modifier.padding(8.dp), tint = Color(0xFF1976D2))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(text = "Room ${order.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                        Text(
                            text = "Order: #${order.id.takeLast(6).uppercase()}", 
                            fontSize = 15.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = Color(0xFF1976D2)
                        )
                    }
                }
                
                StatusBadge(status = order.status)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            
            Text(text = "Placed at $dateTime", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                order.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "${item.quantity}x ${item.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "₹${item.price * item.quantity}", color = Color.Gray, fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "Payment: ${order.paymentMethod}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(text = "Total Amount", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (order.subtotal > 0) {
                            Text(text = "Sub: ₹${order.subtotal} + Tax: ₹${order.tax}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Text(text = "₹${order.totalAmount}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1976D2), fontSize = 18.sp)
                    }
                }

                if (order.assignedStaffName != null) {
                    Surface(
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(8.dp))
                            Text(text = "Assigned to: ${order.assignedStaffName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }

                if (order.notes.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        color = Color(0xFFFFFDE7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Note: ${order.notes}", modifier = Modifier.padding(8.dp), fontSize = 12.sp, color = Color(0xFFF57F17))
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    when (order.status) {
                        OrderStatus.PENDING -> {
                            var isAccepting by remember { mutableStateOf(false) }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Waiting for Restaurant to Accept...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GeminiLoadingButton(
                                        text = "ACCEPT",
                                        isLoading = isAccepting,
                                        onClick = { 
                                            isAccepting = true
                                            onStatusUpdate(order.id, OrderStatus.ACCEPTED)
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        containerColor = Color(0xFF2E7D32)
                                    )
                                    OutlinedButton(
                                        onClick = { onStatusUpdate(order.id, OrderStatus.NOT_ACCEPTED) },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        border = BorderStroke(1.5.dp, Color.Red),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isAccepting
                                    ) { Text("DECLINE", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                        OrderStatus.ACCEPTED -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Assigning to Kitchen...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Button(
                                    onClick = { 
                                        nextStatusAfterAssign = OrderStatus.PROCESSING
                                        showStaffDialog = true 
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                                ) { Text("ASSIGN KITCHEN STAFF", fontWeight = FontWeight.Bold) }
                            }
                        }
                        OrderStatus.PROCESSING -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Food is being prepared...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Button(
                                    onClick = { 
                                        onStatusUpdate(order.id, OrderStatus.READY)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("MARK AS READY", fontWeight = FontWeight.Bold) }
                            }
                        }
                        OrderStatus.READY -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Order ready for pickup. PIN: ${order.deliveryPin}", fontWeight = FontWeight.Bold, color = Color(0xFF00ACC1))
                                Button(
                                    onClick = { 
                                        nextStatusAfterAssign = OrderStatus.PICKED_UP
                                        showStaffDialog = true 
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB8C00)),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("ASSIGN WAITER", fontWeight = FontWeight.Bold) }
                            }
                        }
                        OrderStatus.PICKED_UP -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Out for delivery...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Button(
                                    onClick = { 
                                        onStatusUpdate(order.id, OrderStatus.ARRIVED)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("MARK AS ARRIVED", fontWeight = FontWeight.Bold) }
                            }
                        }
                        OrderStatus.ARRIVED -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = "Verify guest PIN to deliver:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = pinInput,
                                        onValueChange = { pinInput = it; pinError = false },
                                        modifier = Modifier.weight(1f),
                                        isError = pinError,
                                        shape = RoundedCornerShape(12.dp),
                                        placeholder = { Text("4-digit PIN") }
                                    )
                                    Button(
                                        onClick = { 
                                            if (pinInput == order.deliveryPin) {
                                                onStatusUpdate(order.id, OrderStatus.DELIVERED)
                                            } else {
                                                pinError = true
                                            }
                                        },
                                        modifier = Modifier.height(52.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("DELIVER", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

    if (showStaffDialog) {
        val filteredStaff = when (nextStatusAfterAssign) {
            OrderStatus.PROCESSING -> staffList.filter { it.role == "Restaurant" }
            OrderStatus.PICKED_UP -> staffList.filter { it.role == "Waiter" }
            else -> staffList
        }
        
        StaffAssignmentDialog(
            staffList = filteredStaff,
            onDismiss = { showStaffDialog = false },
            onAssign = { staff ->
                onAssignStaff(order.id, staff)
                nextStatusAfterAssign?.let { onStatusUpdate(order.id, it) }
                showStaffDialog = false
            }
        )
    }
}

@Composable
fun SecurityVerificationDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    securityManager: com.example.roomservice.util.SecurityManager
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Try Biometric if enabled
    LaunchedEffect(Unit) {
        if (securityManager.useBiometric()) {
            val activity = context as? androidx.fragment.app.FragmentActivity
            if (activity != null && com.example.roomservice.util.BiometricHelper.isBiometricAvailable(activity)) {
                com.example.roomservice.util.BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    title = "Security Verification",
                    subtitle = "Verify your identity to proceed",
                    onSuccess = { onSuccess() },
                    onError = { /* Fallback to PIN */ }
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Security Verification") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Enter your 6-digit MPIN to continue", color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 6) {
                            pin = it
                            error = false
                        }
                    },
                    label = { Text("MPIN") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    isError = error,
                    modifier = Modifier.width(150.dp)
                )
                if (error) {
                    Text("Invalid PIN!", color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin == securityManager.getPin()) {
                        onSuccess()
                    } else {
                        error = true
                    }
                }
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GeminiLoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_modern")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val geminiColors = listOf(
        Color(0xFF4285F4), Color(0xFF9B51E0), Color(0xFFEA4335),
        Color(0xFFFBBC04), Color(0xFF34A853), Color(0xFF4285F4)
    )

    // Using graphicsLayer to perfectly clip the rotating background
    val buttonShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                shape = buttonShape
                clip = true
            }
            .drawBehind {
                // Background rotating layer
                rotate(rotation) {
                    drawCircle(
                        brush = Brush.sweepGradient(geminiColors),
                        radius = size.width.coerceAtLeast(size.height)
                    )
                }
            }
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Inner Surface to create the border effect
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.5.dp), // Border thickness
            shape = RoundedCornerShape(10.dp),
            color = if (isLoading) containerColor.copy(alpha = 0.9f) else containerColor,
            contentColor = contentColor
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = contentColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = text, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
