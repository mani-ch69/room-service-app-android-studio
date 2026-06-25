package com.example.roomservice.ui.waiter

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import android.media.RingtoneManager
import coil.compose.AsyncImage
import com.example.roomservice.data.model.MenuItem
import com.example.roomservice.data.model.Category
import com.example.roomservice.data.model.OrderStatus
import com.example.roomservice.data.model.Staff
import com.example.roomservice.ui.util.zoomClick
import com.example.roomservice.ui.settings.SettingsScreen
import com.example.roomservice.ui.settings.GeneralSettingsScreen
import com.example.roomservice.ui.settings.BusinessDetailsScreen
import com.example.roomservice.ui.settings.SecuritySettingsScreen
import com.example.roomservice.ui.settings.AppLockSettingsScreen
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(
    staffIdLabel: String = "Staff ID:RS-001",
    staffName: String = "Admin Profile",
    staffPhoto: String? = null,
    onRoomManagementClick: () -> Unit = {},
    onWaiterDashboardClick: () -> Unit = {},
    onChatWithRoomClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    viewModel: AdminMenuViewModel = viewModel()
) {
    val menuItems by viewModel.menuItems.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val roomStatuses by viewModel.roomLiveStatuses.collectAsState(initial = emptyList())
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    
    val staffList by viewModel.staffList.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val rooms = remember(roomStatuses) { roomStatuses.map { it.room } }
    
    var selectedBottomTab by remember { mutableStateOf("home") }
    var moreTabSubScreen by remember { mutableStateOf("main") }
    var selectedMenuCategory by remember { mutableStateOf("All") }
    
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // Handle Device Back Button
    BackHandler {
        when {
            selectedBottomTab == "settings" && moreTabSubScreen != "main" -> {
                moreTabSubScreen = when(moreTabSubScreen) {
                    "general_settings", "security_settings" -> "settings_details"
                    "app_lock" -> "security_settings"
                    "restaurant_edit_menu", "order_trash", "restaurant_orders" -> "restaurant_menu"
                    "restaurant_menu", "staff_mgmt_main", "shops", "payments", "reports", "messaging_preference" -> "main"
                    "shop_edit_menu" -> "shops"
                    "staff_mgmt_profile" -> "staff_mgmt_main"
                    "staff_mgmt_success" -> "main"
                    "hotel_stays", "hotel_rooms_list", "hotel_bookings", "hotel_rates_menu", "hotel_property_detail" -> "hotel_stays"
                    "hotel_availability" -> "hotel_rates_menu"
                    "hotel_prop_general_info", "hotel_prop_vat_tax", "hotel_prop_photos", "hotel_prop_policies", "hotel_prop_res_policies", "hotel_prop_facilities", "hotel_prop_room_details", "hotel_prop_amenities", "hotel_prop_profile", "hotel_prop_descriptions", "hotel_prop_messaging", "hotel_prop_sustainability" -> "hotel_property_detail"
                    else -> "main"
                }
            }
            selectedBottomTab == "orders" -> {
                selectedBottomTab = "settings"
                moreTabSubScreen = "main"
            }
            selectedBottomTab != "home" -> {
                selectedBottomTab = "home"
            }
            else -> {
                activity?.moveTaskToBack(true)
            }
        }
    }
    
    var selectedStaffName by remember { mutableStateOf("") }
    var selectedStaffPhone by remember { mutableStateOf("") }
    var editingStaffId by remember { mutableStateOf<String?>(null) }
    var staffSelectedRole by remember { mutableStateOf("Waiter") }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf<String?>(null) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    val securityManager = remember { com.example.roomservice.util.SecurityManager(context) }
    var showSecurityVerification by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.notificationSignal.collect { message ->
            try {
                val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notification)
                r.play()
                com.example.roomservice.util.NotificationHelper.showNotification(
                    context = context,
                    title = "New Room Service Alert",
                    message = if (message == "Update") "You have new pending requests!" else message
                )
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    var onSecurityVerifiedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    fun requestSecureAction(action: () -> Unit) {
        onSecurityVerifiedAction = action
        showSecurityVerification = true
    }

    val allNotifications by com.example.roomservice.data.NotificationRepository.notifications.collectAsState()
    val unreadNotifCount = allNotifications.count { !it.isRead }
    var showNotificationList by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF1F5F9), // LIGHT GRAY BACKGROUND
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = if (selectedBottomTab == "home") "HOME" else when(selectedBottomTab) {
                        "orders" -> "Orders"
                        "hotel_stays" -> when(moreTabSubScreen) {
                            "hotel_rooms_list" -> "Rooms"
                            "hotel_bookings" -> "Bookings"
                            "hotel_rates_menu" -> "Rates & Availability"
                            "hotel_property_detail" -> "Property Detail"
                            else -> "Hotel & Stay's"
                        }
                        "chat" -> "Messages"
                        "settings" -> when(moreTabSubScreen) {
                            "settings_details" -> "Settings"
                            "general_settings" -> "General"
                            "security_settings" -> "Security"
                            "restaurant_menu" -> "Restaurant"
                            "restaurant_edit_menu" -> "Menu Management"
                            "restaurant_orders" -> "Orders Feed"
                            "order_trash" -> "Order Trash"
                            else -> "More"
                        }
                        else -> "Staff"
                    }
                    Text(text = titleText, fontWeight = FontWeight.Black, color = Color.Black)
                },
                navigationIcon = {},
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedBottomTab == "home",
                    onClick = { selectedBottomTab = "home" },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == "chat",
                    onClick = { selectedBottomTab = "chat" },
                    icon = { 
                        BadgedBox(badge = { if(unreadCount > 0) Badge(containerColor = Color.Gray, modifier = Modifier.size(6.dp)) }) { 
                            Icon(Icons.AutoMirrored.Filled.Chat, null) 
                        } 
                    },
                    label = { Text("Chat", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == "settings",
                    onClick = { selectedBottomTab = "settings"; moreTabSubScreen = "main" },
                    icon = { Icon(Icons.Default.MoreHoriz, null) },
                    label = { Text("More", fontSize = 10.sp) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF1F5F9))) {
            when (selectedBottomTab) {
                "home" -> {
                    val allOrders by viewModel.allOrders.collectAsState()
                    val allCalls by viewModel.allCalls.collectAsState()
                    AdminDashboardContent(
                        stats = dashboardStats,
                        roomStatuses = roomStatuses,
                        onAttendCall = { viewModel.attendCall(it) },
                        onChatClick = onChatWithRoomClick,
                        staffList = staffList,
                        allOrders = allOrders,
                        allCalls = allCalls,
                        bookings = bookings,
                        onStatusUpdate = { id, status ->
                            when (status) {
                                OrderStatus.ACCEPTED -> viewModel.acceptOrder(id)
                                OrderStatus.CANCELLED, OrderStatus.NOT_ACCEPTED -> viewModel.cancelOrder(id)
                                OrderStatus.DELIVERED -> viewModel.completeOrder(id)
                                else -> viewModel.updateOrderStatus(id, status)
                            }
                        },
                        onAssignStaffToCall = { callId, staff -> viewModel.assignStaffToCall(callId, staff) },
                        onAssignStaffToOrder = { orderId, staff -> viewModel.assignStaffToOrder(orderId, staff) },
                        onDeleteBooking = { id -> requestSecureAction { viewModel.deleteBooking(id) } },
                        onAddBooking = { viewModel.addBooking(it) }
                    )
                }
                "orders" -> {
                    val allOrders by viewModel.allOrders.collectAsState()
                    OrdersList(orders = allOrders, staffList = staffList, onStatusUpdate = { id, st -> viewModel.updateOrderStatus(id, st) }, onAssignStaff = { id, s -> viewModel.assignStaffToOrder(id, s) })
                }
                "chat" -> AdminChatListScreen(onChatWithRoomClick, {})
                "settings" -> {
                    when (moreTabSubScreen) {
                        "main" -> AdminMoreContent(
                            staffName = staffName, staffIdLabel = staffIdLabel, staffPhoto = staffPhoto,
                            onRestaurantMenuClick = { moreTabSubScreen = "restaurant_menu" },
                            onHotelStaysClick = { moreTabSubScreen = "hotel_stays" },
                            onShopsClick = { moreTabSubScreen = "shops" },
                            onReportsClick = { moreTabSubScreen = "reports" },
                            onManageStaffClick = { moreTabSubScreen = "staff_mgmt_main" },
                            onSettingsClick = { moreTabSubScreen = "settings_details" },
                            onLogoutClick = onLogoutClick
                        )
                        "restaurant_menu" -> RestaurantMenu(
                            onMenuManagementClick = { moreTabSubScreen = "restaurant_edit_menu" },
                            onOrderTrashClick = { moreTabSubScreen = "order_trash" },
                            onOrdersClick = { moreTabSubScreen = "restaurant_orders" }
                        )
                        "restaurant_edit_menu" -> MenuManagementContent(menuItems, selectedMenuCategory, categoriesList, { selectedMenuCategory = it }, { id, st -> requestSecureAction { viewModel.toggleAvailability(id, st) } }, { id -> requestSecureAction { viewModel.removeItem(id) } }, { id -> requestSecureAction { viewModel.removeCategory(id) } }, { cat -> requestSecureAction { categoryToEdit = cat } }, { requestSecureAction { showAddCategoryDialog = true } }, { cat -> requestSecureAction { showAddItemDialog = cat } })
                        "restaurant_orders" -> {
                            val allOrders by viewModel.allOrders.collectAsState()
                            OrdersList(orders = allOrders, staffList = staffList, onStatusUpdate = { id, st -> viewModel.updateOrderStatus(id, st) }, onAssignStaff = { id, s -> viewModel.assignStaffToOrder(id, s) })
                        }
                        "order_trash" -> {
                            val trashedOrders by viewModel.trashedOrders.collectAsState()
                            OrdersList(orders = trashedOrders, staffList = staffList, onStatusUpdate = { id, st -> viewModel.updateOrderStatus(id, st) })
                        }
                        "hotel_stays" -> HotelStaysMenu({ moreTabSubScreen = "hotel_bookings" }, { moreTabSubScreen = "hotel_rates_menu" }, { moreTabSubScreen = "hotel_property_detail" })
                        "hotel_bookings" -> BookingManagementScreen(bookings, rooms, { moreTabSubScreen = "hotel_stays" }, { viewModel.addBooking(it) }, { id -> requestSecureAction { viewModel.deleteBooking(id) } }, { id, st -> viewModel.updateBookingStatus(id, st) }, { id, url -> viewModel.checkInWithId(id, url) })
                        "hotel_rates_menu" -> RatesAvailabilityMenu(
                            onCalendarClick = { moreTabSubScreen = "hotel_availability" },
                            onItemClick = { /* Other items click */ }
                        )
                        "hotel_availability" -> AvailabilityCalendarScreen(rooms, bookings, { moreTabSubScreen = "hotel_rates_menu" }, { viewModel.addBooking(it) })
                        "hotel_property_detail" -> PropertyDetailMenu({ moreTabSubScreen = "hotel_prop_general_info" }, { moreTabSubScreen = "hotel_prop_vat_tax" }, { moreTabSubScreen = "hotel_prop_photos" }, { moreTabSubScreen = "hotel_prop_policies" }, { moreTabSubScreen = "hotel_prop_res_policies" }, { moreTabSubScreen = "hotel_prop_facilities" }, { moreTabSubScreen = "hotel_rooms_list" }, { moreTabSubScreen = "hotel_prop_amenities" }, { moreTabSubScreen = "hotel_prop_profile" }, { moreTabSubScreen = "hotel_prop_descriptions" }, { moreTabSubScreen = "hotel_prop_messaging" }, { moreTabSubScreen = "hotel_prop_sustainability" })
                        "hotel_rooms_list" -> RoomManagementScreen { moreTabSubScreen = "hotel_property_detail" }
                        "shops" -> ShopMenu(onShopManagementClick = { moreTabSubScreen = "shop_edit_menu" })
                        "shop_edit_menu" -> {
                            val shopItems by viewModel.shopItems.collectAsState()
                            val shopCategories by viewModel.shopCategories.collectAsState()
                            MenuManagementContent(
                                menuItems = shopItems,
                                selectedCategory = selectedMenuCategory,
                                categoriesList = shopCategories,
                                onCategorySelect = { selectedMenuCategory = it },
                                onToggle = { id, st -> requestSecureAction { viewModel.toggleShopItemAvailability(id, st) } },
                                onDelete = { id -> requestSecureAction { viewModel.removeShopItem(id) } },
                                onDeleteCategory = { id -> requestSecureAction { viewModel.removeShopCategory(id) } },
                                onEditCategory = { /* Handle edit if needed */ },
                                onAddCategoryClick = { requestSecureAction { showAddCategoryDialog = true } },
                                onAddItemClick = { cat -> requestSecureAction { showAddItemDialog = cat } }
                            )
                        }
                        "settings_details" -> SettingsScreen({ moreTabSubScreen = "main" }, { moreTabSubScreen = "general_settings" }, { moreTabSubScreen = "security_settings" })
                        "general_settings" -> GeneralSettingsScreen({ moreTabSubScreen = "settings_details" }, onProfileClick)
                        "security_settings" -> SecuritySettingsScreen({ moreTabSubScreen = "settings_details" }, { moreTabSubScreen = "app_lock" })
                        "app_lock" -> AppLockSettingsScreen { moreTabSubScreen = "security_settings" }
                        "staff_mgmt_main" -> StaffManagementScreen(staffList, { role -> editingStaffId = null; selectedStaffName = ""; selectedStaffPhone = ""; staffSelectedRole = role ?: "Waiter"; moreTabSubScreen = "staff_mgmt_profile" }, { staff -> editingStaffId = staff.id; selectedStaffName = staff.name; selectedStaffPhone = staff.phone; staffSelectedRole = staff.role; moreTabSubScreen = "staff_mgmt_profile" }, { id -> viewModel.removeStaff(id) })
                        "staff_mgmt_profile" -> UserProfileScreen(selectedStaffName, { selectedStaffName = it }, selectedStaffPhone, { selectedStaffPhone = it }, staffSelectedRole, { staffSelectedRole = it }, { viewModel.saveStaff(editingStaffId, selectedStaffName, selectedStaffPhone, staffSelectedRole); moreTabSubScreen = "staff_mgmt_success" })
                        "staff_mgmt_success" -> StaffSuccessScreen { moreTabSubScreen = "main" }
                        "payments" -> PaymentsManagementScreen()
                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Coming Soon", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        val isShop = moreTabSubScreen == "shop_edit_menu"
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false }, 
            onConfirm = { 
                if (isShop) viewModel.addNewShopCategory(it) 
                else viewModel.addNewCategory(it)
                showAddCategoryDialog = false 
            }
        )
    }
    if (categoryToEdit != null) {
        // Edit category only for restaurant currently, can be extended for shop
        AddCategoryDialog({ categoryToEdit = null }, { viewModel.editCategory(categoryToEdit!!.id, it); categoryToEdit = null }, categoryToEdit!!.name)
    }
    if (showAddItemDialog != null) {
        val isShop = moreTabSubScreen == "shop_edit_menu"
        AddItemDialog(
            currentCategory = showAddItemDialog!!, 
            isShop = isShop,
            onDismiss = { showAddItemDialog = null }, 
            onConfirm = { n, p, u, c, d, i, s -> 
                if (isShop) viewModel.addNewShopItem(n, p, u, c, d, i, s)
                else viewModel.addNewItem(n, p, u, c, d, i)
                showAddItemDialog = null 
            }
        )
    }
    if (showSecurityVerification) SecurityVerificationDialog({ showSecurityVerification = false }, { showSecurityVerification = false; onSecurityVerifiedAction?.invoke(); onSecurityVerifiedAction = null }, securityManager)
    if (showNotificationList) NotificationListDialog(allNotifications, { showNotificationList = false }, { com.example.roomservice.data.NotificationRepository.markAsRead(it) }, { com.example.roomservice.data.NotificationRepository.markAllAsRead() }, { com.example.roomservice.data.NotificationRepository.clearNotifications() })
}

@Composable
fun AdminMoreContent(staffName: String, staffIdLabel: String, staffPhoto: String?, onRestaurantMenuClick: () -> Unit, onHotelStaysClick: () -> Unit, onShopsClick: () -> Unit, onReportsClick: () -> Unit, onManageStaffClick: () -> Unit, onSettingsClick: () -> Unit, onLogoutClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            if (staffPhoto != null) AsyncImage(model = staffPhoto, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Default.Person, null, modifier = Modifier.padding(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column { Text(staffName, style = MaterialTheme.typography.titleLarge, color = Color.Black); Text(staffIdLabel, style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                    }
                }
            }
            item { MoreMenuItem("Restaurant", Icons.Default.RestaurantMenu, onRestaurantMenuClick) }
            item { MoreMenuItem("Hotel & Stay's", Icons.Default.Hotel, onHotelStaysClick) }
            item { MoreMenuItem("Shops", Icons.Default.Store, onShopsClick) }
            item { MoreMenuItem("Reports", Icons.Default.Assessment, onReportsClick) }
            item { MoreMenuItem("Manage Staff Team", Icons.Default.PersonAdd, onManageStaffClick) }
            item { MoreMenuItem("Settings", Icons.Default.Settings, onSettingsClick) }
        }
        Column(modifier = Modifier.padding(bottom = 16.dp)) { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp); MoreMenuItem("Logout", Icons.AutoMirrored.Filled.Logout, onLogoutClick, Color.Red) }
    }
}

@Composable
fun MoreMenuItem(label: String, icon: ImageVector, onClick: () -> Unit, tint: Color = Color.Black, showNewBadge: Boolean = false) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Color.White) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if(tint == Color.Red) Color.Red else Color(0xFF1976D2), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
            if (showNewBadge) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = Color(0xFF2E7D32),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "New",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))
}

@Composable
fun RestaurantMenu(onMenuManagementClick: () -> Unit, onOrderTrashClick: () -> Unit, onOrdersClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        MoreMenuItem("Orders Feed", Icons.AutoMirrored.Filled.Assignment, onOrdersClick)
        MoreMenuItem("Menu Management", Icons.Default.RestaurantMenu, onMenuManagementClick)
        MoreMenuItem("Order Trash", Icons.Default.DeleteSweep, onOrderTrashClick)
    }
}

@Composable
fun HotelStaysMenu(onBookingsClick: () -> Unit, onRatesClick: () -> Unit, onPropertyClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        MoreMenuItem("Bookings", Icons.Default.BookOnline, onBookingsClick)
        MoreMenuItem("Rates & Availability", Icons.Default.EventAvailable, onRatesClick)
        MoreMenuItem("Property Detail", Icons.Default.Business, onPropertyClick)
    }
}

@Composable
fun RatesAvailabilityMenu(onCalendarClick: () -> Unit, onItemClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        item { MoreMenuItem("Calendar", Icons.Default.CalendarMonth, onCalendarClick) }
        item { MoreMenuItem("Open/close rooms", Icons.Default.DoorBack, { onItemClick("open_close") }) }
        item { MoreMenuItem("Copy rates to future dates", Icons.Default.ContentCopy, { onItemClick("copy_rates") }) }
        item { MoreMenuItem("Dynamic Restriction Rules", Icons.Default.Gavel, { onItemClick("restriction_rules") }, showNewBadge = true) }
        item { MoreMenuItem("Sync calendars", Icons.Default.Sync, { onItemClick("sync_calendars") }) }
        item { MoreMenuItem("Rate plans", Icons.Default.ListAlt, { onItemClick("rate_plans") }) }
        item { MoreMenuItem("Value adds", Icons.Default.AddCircleOutline, { onItemClick("value_adds") }, showNewBadge = true) }
        item { MoreMenuItem("Availability planner", Icons.Default.EventNote, { onItemClick("planner") }) }
        item { MoreMenuItem("Pricing per guest", Icons.Default.Groups, { onItemClick("pricing_guest") }) }
        item { MoreMenuItem("Country rates", Icons.Default.Public, { onItemClick("country_rates") }, showNewBadge = true) }
        item { MoreMenuItem("Mobile rates", Icons.Default.Smartphone, { onItemClick("mobile_rates") }) }
    }
}

@Composable
fun ShopMenu(onShopManagementClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        MoreMenuItem("Shop Management", Icons.Default.Store, onShopManagementClick)
    }
}

@Composable
fun PropertyDetailMenu(onGeneralInfoClick: () -> Unit, onVatTaxClick: () -> Unit, onPhotosClick: () -> Unit, onPoliciesClick: () -> Unit, onResPoliciesClick: () -> Unit, onFacilitiesClick: () -> Unit, onRoomDetailsClick: () -> Unit, onAmenitiesClick: () -> Unit, onProfileClick: () -> Unit, onDescriptionsClick: () -> Unit, onMessagingClick: () -> Unit, onSustainabilityClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        item { MoreMenuItem("Property Page Score", Icons.Default.Score, { }) }
        item { MoreMenuItem("General info & property status", Icons.Default.Info, onGeneralInfoClick) }
        item { MoreMenuItem("VAT/Tax/Charges", Icons.Default.Receipt, onVatTaxClick) }
        item { MoreMenuItem("Photos", Icons.Default.PhotoLibrary, onPhotosClick) }
        item { MoreMenuItem("Property policies", Icons.Default.Policy, onPoliciesClick) }
        item { MoreMenuItem("Reservation policies", Icons.Default.HistoryEdu, onResPoliciesClick) }
        item { MoreMenuItem("Facilities & services", Icons.Default.RoomService, onFacilitiesClick) }
        item { MoreMenuItem("Rooms", Icons.Default.Bed, onRoomDetailsClick) }
        item { MoreMenuItem("Room Amenities", Icons.Default.KingBed, onAmenitiesClick) }
        item { MoreMenuItem("Your Profile", Icons.Default.AccountCircle, onProfileClick) }
        item { MoreMenuItem("View Your Descriptions", Icons.Default.Description, onDescriptionsClick) }
        item { MoreMenuItem("Messaging Preferences", Icons.Default.Chat, onMessagingClick) }
        item { MoreMenuItem("Sustainability", Icons.Default.Eco, onSustainabilityClick) }
    }
}

@Composable
fun AdminDashboardRooms(statuses: List<RoomLiveStatus>, onReceiveCall: (String) -> Unit, onChatClick: (String) -> Unit, staffList: List<Staff>, onAssignStaffToCall: (String, Staff) -> Unit) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        items(statuses.size) { index ->
            val status = statuses[index]
            RoomStatusCard(status = status, isCompact = true, onAttend = { status.activeCall?.let { onReceiveCall(it.id) } }, onChat = { onChatClick(status.room.roomNumber) }, onReceiveCall = onReceiveCall, staffList = staffList, onAssignStaffToCall = { staff -> status.activeCall?.let { onAssignStaffToCall(it.id, staff) } })
        }
    }
}

@Composable
fun MenuManagementContent(menuItems: List<MenuItem>, selectedCategory: String, categoriesList: List<Category>, onCategorySelect: (String) -> Unit, onToggle: (String, Boolean) -> Unit, onDelete: (String) -> Unit, onDeleteCategory: (String) -> Unit, onEditCategory: (Category) -> Unit, onAddCategoryClick: () -> Unit, onAddItemClick: (String) -> Unit) {
    val filteredItems = if (selectedCategory == "All") menuItems else menuItems.filter { it.category == selectedCategory }
    val categories = listOf("All") + categoriesList.map { it.name }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        LazyRow(modifier = Modifier.fillMaxWidth().background(Color.White), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            item { IconButton(onClick = onAddCategoryClick, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).size(40.dp)) { Icon(Icons.Default.Add, "Add Category", tint = MaterialTheme.colorScheme.primary) } }
            items(categories) { categoryName ->
                var showDeleteMenu by remember { mutableStateOf(false) }
                Surface(selected = selectedCategory == categoryName, onClick = { onCategorySelect(categoryName) }, shape = RoundedCornerShape(20.dp), color = if (selectedCategory == categoryName) MaterialTheme.colorScheme.primary else Color.White, border = if (selectedCategory == categoryName) null else BorderStroke(1.dp, Color.LightGray), modifier = Modifier.height(40.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(categoryName, color = if (selectedCategory == categoryName) Color.White else Color.Black, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        if (categoryName != "All") { Spacer(modifier = Modifier.width(6.dp)); Icon(Icons.Default.MoreVert, null, Modifier.size(16.dp).clickable { showDeleteMenu = true }, tint = if (selectedCategory == categoryName) Color.White else Color.Gray) }
                    }
                }
                DropdownMenu(expanded = showDeleteMenu, onDismissRequest = { showDeleteMenu = false }) { DropdownMenuItem(text = { Text("Add Item") }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = { onAddItemClick(categoryName); showDeleteMenu = false }); DropdownMenuItem(text = { Text("Edit Category") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { val categoryObj = categoriesList.find { it.name == categoryName }; categoryObj?.let { onEditCategory(it) }; showDeleteMenu = false }); DropdownMenuItem(text = { Text("Delete Category", color = Color.Red) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }, onClick = { val categoryObj = categoriesList.find { it.name == categoryName }; categoryObj?.let { onDeleteCategory(it.id) }; showDeleteMenu = false; if (selectedCategory == categoryName) onCategorySelect("All") }) }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(filteredItems) { AdminMenuItemRow(item = it, onToggle = { st -> onToggle(it.id, it.isAvailable) }, onDelete = { onDelete(it.id) }) } }
    }
}

@Composable
fun AdminMenuItemRow(item: MenuItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) { 
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                var subText = "₹${item.price} / ${item.unit} • ${item.category}"
                if (item.itemType == "SHOP" && item.stock >= 0) {
                    subText += " • Stock: ${item.stock}"
                }
                Text(text = subText, color = Color.Gray, fontSize = 13.sp) 
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = item.isAvailable, onCheckedChange = onToggle, modifier = Modifier.scale(0.7f), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2E7D32), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color.Red.copy(alpha = 0.5f), uncheckedBorderColor = Color.Red)); Spacer(Modifier.width(8.dp)); IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f)) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(currentCategory: String, isShop: Boolean = false, onDismiss: () -> Unit, onConfirm: (String, Double, String, String, String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("plate") }
    var unitExpanded by remember { mutableStateOf(false) }
    val units = listOf("liter", "kg", "pc", "plate")
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val cameraImageUri = remember { try { val file = File(context.cacheDir, "temp_img.jpg"); FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) } catch (e: Exception) { null } }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if (it) selectedImageUri = cameraImageUri }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) selectedImageUri = it }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it && cameraImageUri != null) cameraLauncher.launch(cameraImageUri) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Item to $currentCategory", color = Color.Black) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f))
                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = !unitExpanded }, Modifier.weight(1f)) {
                    OutlinedTextField(value = selectedUnit, onValueChange = {}, readOnly = true, label = { Text("Unit") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) { units.forEach { unit -> DropdownMenuItem(text = { Text(unit) }, onClick = { selectedUnit = unit; unitExpanded = false }) } }
                }
            }
            if (isShop) {
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Current Stock") })
            }
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraLauncher.launch(cameraImageUri!!) else permissionLauncher.launch(Manifest.permission.CAMERA) }, Modifier.weight(1f)) { Text("Camera") }
                Button(onClick = { galleryLauncher.launch("image/*") }, Modifier.weight(1f)) { Text("Gallery") }
            }
            if (selectedImageUri != null) AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
        }
    }, confirmButton = { Button(onClick = { onConfirm(name, price.toDoubleOrNull() ?: 0.0, selectedUnit, currentCategory, description, selectedImageUri?.toString() ?: "", stock.toIntOrNull() ?: -1) }, enabled = name.isNotBlank() && price.isNotBlank()) { Text("Add Item") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit, initialName: String = "") {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initialName.isEmpty()) "Create New Category" else "Edit Category", color = Color.Black) }, text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text(if (initialName.isEmpty()) "Create" else "Update") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
