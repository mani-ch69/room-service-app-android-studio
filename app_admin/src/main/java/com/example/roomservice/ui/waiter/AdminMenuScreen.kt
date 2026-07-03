package com.example.roomservice.ui.waiter

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.roomservice.data.model.BookingStatus
import com.example.roomservice.ui.settings.SettingsScreen
import com.example.roomservice.ui.settings.GeneralSettingsScreen
import com.example.roomservice.ui.settings.SecuritySettingsScreen
import com.example.roomservice.ui.settings.AppLockSettingsScreen
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(
    staffName: String = "Admin Profile",
    staffIdLabel: String = "Admin ID",
    staffPhoto: String? = null,
    onProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    viewModel: AdminMenuViewModel = viewModel()
) {
    val roomStatuses by viewModel.roomLiveStatuses.collectAsState(initial = emptyList())
    val bookings by viewModel.bookings.collectAsState()
    val rooms = remember(roomStatuses) { roomStatuses.map { it.room } }
    
    var selectedBottomTab by remember { mutableStateOf("home") }
    var moreTabSubScreen by remember { mutableStateOf("main") }
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    BackHandler {
        if (selectedBottomTab == "settings" && moreTabSubScreen != "main") {
            moreTabSubScreen = when(moreTabSubScreen) {
                "general_settings", "security_settings" -> "settings_details"
                "app_lock" -> "security_settings"
                "hotel_stays" -> "main"
                "hotel_rooms_list", "hotel_bookings", "hotel_rates_menu", "hotel_property_detail", "hotel_availability", "pricing_guest" -> "hotel_stays"
                "hotel_prop_general_info", "hotel_prop_vat_tax", "hotel_prop_photos", "hotel_prop_policies", "hotel_prop_res_policies", "hotel_prop_facilities", "hotel_prop_room_details", "hotel_prop_amenities", "hotel_prop_profile", "hotel_prop_descriptions", "hotel_prop_messaging", "hotel_prop_sustainability" -> "hotel_property_detail"
                else -> "main"
            }
        } else if (selectedBottomTab != "home") {
            selectedBottomTab = "home"
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    var showAddBookingDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            TopAppBar(
                title = { 
                    val title = if (selectedBottomTab == "home") "DASHBOARD" else when(moreTabSubScreen) {
                        "hotel_stays" -> "Hotel & Stay's"
                        "hotel_rooms_list" -> "Room Detail"
                        "hotel_bookings" -> "Bookings"
                        "hotel_rates_menu" -> "Rates & Availability"
                        "hotel_property_detail" -> "Property Detail"
                        "hotel_prop_general_info" -> "General Info"
                        "hotel_availability" -> "Availability Planner"
                        "pricing_guest" -> "Pricing per guest"
                        "settings_details" -> "Settings"
                        "general_settings" -> "General Setting"
                        "security_settings" -> "App Security"
                        "app_lock" -> "App Lock"
                        else -> "More"
                    }
                    Text(text = title, fontWeight = FontWeight.Black)
                },
                navigationIcon = {
                    if (selectedBottomTab == "settings" && moreTabSubScreen != "main") {
                        IconButton(onClick = {
                            moreTabSubScreen = when(moreTabSubScreen) {
                                "general_settings", "security_settings" -> "settings_details"
                                "app_lock" -> "security_settings"
                                "hotel_stays" -> "main"
                                "hotel_rooms_list", "hotel_bookings", "hotel_rates_menu", "hotel_property_detail", "hotel_availability", "pricing_guest" -> "hotel_stays"
                                "hotel_prop_general_info", "hotel_prop_vat_tax", "hotel_prop_photos", "hotel_prop_policies", "hotel_prop_res_policies", "hotel_prop_facilities", "hotel_prop_room_details", "hotel_prop_amenities", "hotel_prop_profile", "hotel_prop_descriptions", "hotel_prop_messaging", "hotel_prop_sustainability" -> "hotel_property_detail"
                                else -> "main"
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    }
                },
                actions = {
                    if (moreTabSubScreen == "hotel_rooms_list") IconButton(onClick = { showAddRoomDialog = true }) { Icon(Icons.Default.Add, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Box {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = selectedBottomTab == "home", onClick = { selectedBottomTab = "home" }, icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Home", fontSize = 10.sp) })
                    
                    Spacer(Modifier.weight(1f))

                    NavigationBarItem(selected = selectedBottomTab == "settings", onClick = { selectedBottomTab = "settings"; moreTabSubScreen = "main" }, icon = { Icon(Icons.Default.Menu, null) }, label = { Text("More", fontSize = 10.sp) })
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-10).dp)
                        .zIndex(1f)
                ) {
                    AuroraManualBookingButton(onClick = { showAddBookingDialog = true })
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedBottomTab) {
                "home" -> AdminDashboardContent(
                    roomStatuses = roomStatuses, 
                    bookings = bookings, 
                    onAddBooking = { viewModel.addBooking(it) },
                    onDeleteBooking = { viewModel.deleteBooking(it) }
                )
                "settings" -> {
                    when (moreTabSubScreen) {
                        "main" -> AdminMoreContent(
                            staffName = staffName, staffIdLabel = staffIdLabel, staffPhoto = staffPhoto,
                            onHotelStaysClick = { moreTabSubScreen = "hotel_stays" },
                            onSettingsClick = { moreTabSubScreen = "settings_details" },
                            onLogoutClick = onLogoutClick
                        )
                        "hotel_stays" -> HotelStaysMenu({ moreTabSubScreen = "hotel_bookings" }, { moreTabSubScreen = "hotel_rates_menu" }, { moreTabSubScreen = "hotel_property_detail" })
                        "hotel_bookings" -> {
                            BookingManagementScreen(bookings, rooms) { viewModel.deleteBooking(it) }
                        }
                        "hotel_rates_menu" -> RatesAvailabilityMenu({ moreTabSubScreen = "hotel_availability" }, { 
                            moreTabSubScreen = when(it) {
                                "open_close" -> "hotel_open_close"
                                "pricing_guest" -> "pricing_guest"
                                else -> moreTabSubScreen
                            }
                        })
                        "hotel_open_close" -> OpenCloseRoomsScreen(rooms, { moreTabSubScreen = "hotel_rates_menu" }, { from, to, days, types, status ->
                            // TODO: Implement actual database update logic for bulk open/close
                            moreTabSubScreen = "hotel_rates_menu"
                        })
                        "pricing_guest" -> PricingPerGuestScreen(rooms, { moreTabSubScreen = "hotel_rates_menu" })
                        "hotel_availability" -> AvailabilityCalendarScreen(rooms, bookings, { moreTabSubScreen = "hotel_rates_menu" }, { viewModel.addBooking(it) })
                        "hotel_property_detail" -> PropertyDetailMenu({ moreTabSubScreen = "hotel_prop_general_info" }, { moreTabSubScreen = "hotel_prop_vat_tax" }, { moreTabSubScreen = "hotel_prop_photos" }, { moreTabSubScreen = "hotel_prop_policies" }, { moreTabSubScreen = "hotel_prop_res_policies" }, { moreTabSubScreen = "hotel_prop_facilities" }, { moreTabSubScreen = "hotel_rooms_list" }, { moreTabSubScreen = "hotel_prop_amenities" }, { moreTabSubScreen = "hotel_prop_profile" }, { moreTabSubScreen = "hotel_prop_descriptions" }, { moreTabSubScreen = "hotel_prop_messaging" }, { moreTabSubScreen = "hotel_prop_sustainability" })
                        "hotel_prop_general_info" -> GeneralInfoStatusScreen()
                        "hotel_prop_amenities" -> RoomAmenitiesScreen()
                        "hotel_rooms_list" -> {
                            RoomManagementScreen { moreTabSubScreen = "hotel_property_detail" }
                            if (showAddRoomDialog) AddEditRoomDialog(rooms, null, { showAddRoomDialog = false }, { com.example.roomservice.data.RoomRepository.addRoom(it); showAddRoomDialog = false })
                        }
                        "settings_details" -> SettingsScreen({ moreTabSubScreen = "main" }, { moreTabSubScreen = "security_settings" })
                        "general_settings" -> GeneralSettingsScreen({ moreTabSubScreen = "settings_details" }, onProfileClick)
                        "security_settings" -> SecuritySettingsScreen({ moreTabSubScreen = "settings_details" }, { moreTabSubScreen = "app_lock" })
                        "app_lock" -> AppLockSettingsScreen { moreTabSubScreen = "security_settings" }
                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Coming Soon") }
                    }
                }
            }

            if (showAddBookingDialog) {
                AddBookingDialog(
                    rooms = rooms,
                    onDismiss = { showAddBookingDialog = false },
                    onConfirm = { 
                        viewModel.addBooking(it)
                        showAddBookingDialog = false 
                    }
                )
            }
        }
    }
}

@Composable
fun AuroraManualBookingButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp),
        shape = CircleShape,
        color = Color(0xFF1976D2), // Primary Blue
        shadowElevation = 6.dp,
        border = BorderStroke(2.dp, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Add, 
                contentDescription = null, 
                tint = Color.White, 
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun AdminMoreContent(staffName: String, staffIdLabel: String, staffPhoto: String?, onHotelStaysClick: () -> Unit, onSettingsClick: () -> Unit, onLogoutClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    if (staffPhoto != null) AsyncImage(model = staffPhoto, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.Person, null, modifier = Modifier.padding(16.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column { Text(staffName, fontWeight = FontWeight.Bold); Text(staffIdLabel, fontSize = 12.sp, color = Color.Gray) }
            }
        }
        MoreMenuItem("Hotel & Stay's", Icons.Default.Hotel, onHotelStaysClick)
        MoreMenuItem("Settings", Icons.Default.Settings, onSettingsClick)
        Spacer(Modifier.weight(1f))
        MoreMenuItem("Logout", Icons.AutoMirrored.Filled.Logout, onLogoutClick, Color.Red)
    }
}

@Composable
fun MoreMenuItem(label: String, icon: ImageVector, onClick: () -> Unit, tint: Color = Color.Black, isNew: Boolean = false) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Color.White) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if(tint == Color.Red) Color.Red else Color(0xFF1976D2))
            Spacer(Modifier.width(16.dp))
            Text(label, color = tint, modifier = Modifier.weight(1f))
            if (isNew) {
                Surface(
                    color = Color(0xFF2E7D32),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "New",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
}

@Composable
fun HotelStaysMenu(onBookingsClick: () -> Unit, onRatesClick: () -> Unit, onPropertyClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        MoreMenuItem("Bookings", Icons.Default.BookOnline, onBookingsClick)
        MoreMenuItem("Rates & Availability", Icons.Default.EventAvailable, onRatesClick)
        MoreMenuItem("Property Detail", Icons.Default.Business, onPropertyClick)
    }
}

@Composable
fun RatesAvailabilityMenu(onCalendarClick: () -> Unit, onItemClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { MoreMenuItem("Calendar", Icons.Default.CalendarMonth, onCalendarClick) }
        item { MoreMenuItem("Open/close rooms", Icons.Default.DoorFront, { onItemClick("open_close") }) }
        item { MoreMenuItem("Copy rates to future dates", Icons.Default.ContentCopy, { onItemClick("copy_rates") }) }
        item { MoreMenuItem("Dynamic Restriction Rules", Icons.Default.Gavel, { onItemClick("restriction_rules") }, isNew = true) }
        item { MoreMenuItem("Sync calendars", Icons.Default.Sync, { onItemClick("sync_calendars") }) }
        item { MoreMenuItem("Rate plans", Icons.Default.Assignment, { onItemClick("rate_plans") }) }
        item { MoreMenuItem("Value adds", Icons.Default.AddCircle, { onItemClick("value_adds") }, isNew = true) }
        item { MoreMenuItem("Availability planner", Icons.Default.EventNote, { onItemClick("planner") }) }
        item { MoreMenuItem("Pricing per guest", Icons.Default.Groups, { onItemClick("pricing_guest") }) }
        item { MoreMenuItem("Country rates", Icons.Default.Public, { onItemClick("country_rates") }, isNew = true) }
        item { MoreMenuItem("Mobile rates", Icons.Default.Smartphone, { onItemClick("mobile_rates") }) }
    }
}


@Composable
fun PropertyDetailMenu(onGeneralInfoClick: () -> Unit, onVatTaxClick: () -> Unit, onPhotosClick: () -> Unit, onPoliciesClick: () -> Unit, onResPoliciesClick: () -> Unit, onFacilitiesClick: () -> Unit, onRoomDetailsClick: () -> Unit, onAmenitiesClick: () -> Unit, onProfileClick: () -> Unit, onDescriptionsClick: () -> Unit, onMessagingClick: () -> Unit, onSustainabilityClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { MoreMenuItem("Property Page Score", Icons.Default.Assessment, { /* Score logic */ }) }
        item { MoreMenuItem("General info & property status", Icons.Default.Info, onGeneralInfoClick) }
        item { MoreMenuItem("VAT/Tax/Charges", Icons.Default.Payments, onVatTaxClick) }
        item { MoreMenuItem("Photos", Icons.Default.PhotoLibrary, onPhotosClick) }
        item { MoreMenuItem("Property policies", Icons.Default.Gavel, onPoliciesClick, isNew = true) }
        item { MoreMenuItem("Reservation policies", Icons.Default.EventNote, onResPoliciesClick, isNew = true) }
        item { MoreMenuItem("Facilities & services", Icons.Default.RoomService, onFacilitiesClick) }
        item { MoreMenuItem("Room details", Icons.Default.Bed, onRoomDetailsClick) }
        item { MoreMenuItem("Room Amenities", Icons.Default.HotTub, onAmenitiesClick) }
        item { MoreMenuItem("Your Profile", Icons.Default.AccountBox, onProfileClick) }
        item { MoreMenuItem("View Your Descriptions", Icons.Default.Description, onDescriptionsClick) }
        item { MoreMenuItem("Messaging Preferences", Icons.Default.Message, onMessagingClick) }
        item { MoreMenuItem("Sustainability", Icons.Default.Eco, onSustainabilityClick, isNew = true) }
    }
}
