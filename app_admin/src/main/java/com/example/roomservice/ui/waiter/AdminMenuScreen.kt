package com.example.roomservice.ui.waiter

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import com.example.roomservice.ui.util.AuroraBackground
import com.example.roomservice.ui.util.GlassCard
import com.example.roomservice.ui.util.GlassTextStyle

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
                "general_settings", "security_settings", "inbox" -> "settings_details"
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    val title = when (selectedBottomTab) {
                        "home" -> "DASHBOARD"
                        "reservations" -> "RESERVATIONS"
                        "payments" -> "PAYMENTS"
                        else -> {
                            when(moreTabSubScreen) {
                                "hotel_stays" -> "Hotel & Stay's"
                                "hotel_rooms_list" -> "Room Detail"
                                "hotel_bookings" -> "Reservations"
                                "hotel_rates_menu" -> "Rates & Availability"
                                "hotel_prop_general_info" -> "General Info"
                                "hotel_prop_vat_tax" -> "VAT/Tax/Charges"
                                "hotel_prop_photos" -> "Photos"
                                "hotel_prop_policies" -> "Property Policies"
                                "hotel_prop_res_policies" -> "Reservation Policies"
                                "hotel_prop_amenities" -> "Room Amenities"
                                "hotel_prop_profile" -> "Your Profile"
                                "hotel_prop_messaging" -> "Messaging Preferences"
                                "hotel_availability" -> "Calendar"
                                "pricing_guest" -> "Pricing per guest"
                                "settings_details" -> "Settings"
                                "general_settings" -> "General Setting"
                                "security_settings" -> "App Security"
                                "app_lock" -> "App Lock"
                                "inbox" -> "INBOX"
                                else -> "MORE"
                            }
                        }
                    }
                    Text(
                        text = title, 
                        fontWeight = FontWeight.Black, 
                        color = Color.White,
                        style = GlassTextStyle
                    )
                },
                navigationIcon = {
                    if (selectedBottomTab == "settings" && moreTabSubScreen != "main") {
                        IconButton(onClick = {
                            moreTabSubScreen = when(moreTabSubScreen) {
                                "general_settings", "security_settings", "inbox" -> "settings_details"
                                "app_lock" -> "security_settings"
                                "hotel_stays" -> "main"
                                "hotel_rooms_list", "hotel_bookings", "hotel_rates_menu", "hotel_property_detail", "hotel_availability", "pricing_guest" -> "hotel_stays"
                                "hotel_prop_general_info", "hotel_prop_vat_tax", "hotel_prop_photos", "hotel_prop_policies", "hotel_prop_res_policies", "hotel_prop_facilities", "hotel_prop_room_details", "hotel_prop_amenities", "hotel_prop_profile", "hotel_prop_descriptions", "hotel_prop_messaging", "hotel_prop_sustainability" -> "hotel_property_detail"
                                else -> "main"
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                    }
                },
                actions = {
                    if (moreTabSubScreen == "hotel_rooms_list") IconButton(onClick = { showAddRoomDialog = true }) { Icon(Icons.Default.Add, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.05f))
            )
        },
        bottomBar = {
            Box {
                NavigationBar(containerColor = Color.White.copy(alpha = 0.05f)) {
                    NavigationBarItem(
                        selected = selectedBottomTab == "home", 
                        onClick = { selectedBottomTab = "home" }, 
                        icon = { Icon(Icons.Default.Dashboard, null) }, 
                        label = { Text("Home", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.White.copy(alpha = 0.5f), selectedTextColor = Color.White, unselectedTextColor = Color.White.copy(alpha = 0.5f), indicatorColor = Color.White.copy(alpha = 0.1f))
                    )
                    
                    NavigationBarItem(
                        selected = selectedBottomTab == "reservations", 
                        onClick = { selectedBottomTab = "reservations" }, 
                        icon = { Icon(Icons.Default.BookOnline, null) }, 
                        label = { Text("Reservations", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.White.copy(alpha = 0.5f), selectedTextColor = Color.White, unselectedTextColor = Color.White.copy(alpha = 0.5f), indicatorColor = Color.White.copy(alpha = 0.1f))
                    )
                    
                    Spacer(Modifier.weight(0.5f))

                    NavigationBarItem(
                        selected = selectedBottomTab == "payments", 
                        onClick = { selectedBottomTab = "payments" }, 
                        icon = { Icon(Icons.Default.Payments, null) }, 
                        label = { Text("Payments", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.White.copy(alpha = 0.5f), selectedTextColor = Color.White, unselectedTextColor = Color.White.copy(alpha = 0.5f), indicatorColor = Color.White.copy(alpha = 0.1f))
                    )

                    NavigationBarItem(
                        selected = selectedBottomTab == "settings", 
                        onClick = { selectedBottomTab = "settings"; moreTabSubScreen = "main" }, 
                        icon = { Icon(Icons.Default.Menu, null) }, 
                        label = { Text("More", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.White.copy(alpha = 0.5f), selectedTextColor = Color.White, unselectedTextColor = Color.White.copy(alpha = 0.5f), indicatorColor = Color.White.copy(alpha = 0.1f))
                    )
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-5).dp)
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
                    onDeleteBooking = { viewModel.deleteBooking(it) }
                )
                "reservations" -> BookingManagementScreen(bookings, rooms) { viewModel.deleteBooking(it) }
                "payments" -> PaymentsScreen(bookings = bookings, rooms = rooms)
                "settings" -> {
                    when (moreTabSubScreen) {
                        "main" -> AdminMoreContent(
                            staffName = staffName, staffIdLabel = staffIdLabel, staffPhoto = staffPhoto,
                            onHotelStaysClick = { moreTabSubScreen = "hotel_stays" },
                            onSettingsClick = { moreTabSubScreen = "settings_details" },
                            onLogoutClick = onLogoutClick
                        )
                        "hotel_stays" -> HotelStaysMenu(
                            onBookingsClick = { moreTabSubScreen = "hotel_bookings" },
                            onRatesClick = { moreTabSubScreen = "hotel_rates_menu" },
                            onPropertyClick = { moreTabSubScreen = "hotel_property_detail" }
                        )
                        "hotel_bookings" -> {
                            BookingManagementScreen(bookings, rooms) { viewModel.deleteBooking(it) }
                        }
                        "hotel_rates_menu" -> RatesAvailabilityMenu(
                            onCalendarClick = { moreTabSubScreen = "hotel_availability" },
                            onItemClick = { 
                                moreTabSubScreen = when(it) {
                                    "pricing_guest" -> "pricing_guest"
                                    else -> moreTabSubScreen
                                }
                            }
                        )
                        "pricing_guest" -> PricingPerGuestScreen(rooms, { moreTabSubScreen = "hotel_rates_menu" })
                        "hotel_availability" -> CalendarManagementScreen(rooms, bookings)
                        "hotel_property_detail" -> PropertyDetailMenu({ moreTabSubScreen = "hotel_prop_general_info" }, { moreTabSubScreen = "hotel_prop_vat_tax" }, { moreTabSubScreen = "hotel_prop_photos" }, { moreTabSubScreen = "hotel_prop_policies" }, { moreTabSubScreen = "hotel_prop_res_policies" }, { moreTabSubScreen = "hotel_prop_facilities" }, { moreTabSubScreen = "hotel_rooms_list" }, { moreTabSubScreen = "hotel_prop_amenities" }, { moreTabSubScreen = "hotel_prop_profile" }, { moreTabSubScreen = "hotel_prop_descriptions" }, { moreTabSubScreen = "hotel_prop_messaging" }, { moreTabSubScreen = "hotel_prop_sustainability" })
                        "hotel_prop_general_info" -> GeneralInfoStatusScreen()
                        "hotel_prop_vat_tax" -> VatTaxChargesScreen()
                        "hotel_prop_photos" -> PropertyPhotosScreen()
                        "hotel_prop_policies" -> PropertyPoliciesScreen()
                        "hotel_prop_res_policies" -> ReservationPoliciesScreen()
                        "hotel_prop_amenities" -> RoomAmenitiesScreen()
                        "hotel_prop_profile" -> YourProfileScreen()
                        "hotel_prop_messaging" -> MessagingPreferencesScreen()
                        "hotel_rooms_list" -> {
                            RoomManagementScreen { moreTabSubScreen = "hotel_property_detail" }
                            if (showAddRoomDialog) AddEditRoomDialog(rooms, null, { showAddRoomDialog = false }, { com.example.roomservice.data.RoomRepository.addRoom(it); showAddRoomDialog = false })
                        }
                        "settings_details" -> SettingsScreen({ moreTabSubScreen = "main" }, { moreTabSubScreen = "general_settings" }, { moreTabSubScreen = "security_settings" }, onInboxClick = { moreTabSubScreen = "inbox" })
                        "general_settings" -> GeneralSettingsScreen({ moreTabSubScreen = "settings_details" }, onProfileClick)
                        "security_settings" -> SecuritySettingsScreen({ moreTabSubScreen = "settings_details" }, { moreTabSubScreen = "app_lock" })
                        "app_lock" -> AppLockSettingsScreen { moreTabSubScreen = "security_settings" }
                        "inbox" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Inbox Coming Soon", color = Color.Gray) }
                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Coming Soon") }
                    }
                }
            }

            if (showAddBookingDialog) {
                AddBookingDialog(
                    rooms = rooms,
                    onDismiss = { showAddBookingDialog = false },
                    onConfirm = { 
                        viewModel.addBooking(it, context)
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
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
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
fun AdminMoreContent(
    staffName: String, 
    staffIdLabel: String, 
    staffPhoto: String?, 
    onHotelStaysClick: () -> Unit, 
    onSettingsClick: () -> Unit, 
    onLogoutClick: () -> Unit
) {
    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PROFILE CARD
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        if (staffPhoto != null) {
                            AsyncImage(
                                model = staffPhoto,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person, 
                                null, 
                                modifier = Modifier.padding(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(
                            staffName, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            staffIdLabel, 
                            fontSize = 13.sp, 
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // MENU GROUPS
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column {
                    MoreMenuItem("Hotel & Stay's", Icons.Default.Hotel, onHotelStaysClick)
                    MoreMenuItem("Settings", Icons.Default.Settings, onSettingsClick)
                }
            }

            Spacer(Modifier.weight(1f))

            // LOGOUT BUTTON
            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F).copy(alpha = 0.2f),
                    contentColor = Color(0xFFE57373)
                ),
                border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.3f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(Modifier.width(12.dp))
                Text("Logout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun MoreMenuItem(label: String, icon: ImageVector, onClick: () -> Unit, tint: Color = Color.White, isNew: Boolean = false) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if(tint == Color.Red || tint == Color(0xFFE57373)) Color(0xFFE57373) else Color(0xFF90CAF9))
            Spacer(Modifier.width(16.dp))
            Text(label, color = tint, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            if (isNew) {
                Surface(
                    color = Color(0xFF81C784),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "New",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
}

@Composable
fun HotelStaysMenu(onBookingsClick: () -> Unit, onRatesClick: () -> Unit, onPropertyClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        MoreMenuItem("Reservations", Icons.Default.BookOnline, onBookingsClick)
        MoreMenuItem("Rates & Availability", Icons.Default.EventAvailable, onRatesClick)
        MoreMenuItem("Property Detail", Icons.Default.Business, onPropertyClick)
    }
}

@Composable
fun RatesAvailabilityMenu(onCalendarClick: () -> Unit, onItemClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { MoreMenuItem("Calendar", Icons.Default.CalendarMonth, onCalendarClick) }
        item { MoreMenuItem("Pricing per guest", Icons.Default.Groups, { onItemClick("pricing_guest") }) }
    }
}


@Composable
fun PropertyDetailMenu(onGeneralInfoClick: () -> Unit, onVatTaxClick: () -> Unit, onPhotosClick: () -> Unit, onPoliciesClick: () -> Unit, onResPoliciesClick: () -> Unit, onFacilitiesClick: () -> Unit, onRoomDetailsClick: () -> Unit, onAmenitiesClick: () -> Unit, onProfileClick: () -> Unit, onDescriptionsClick: () -> Unit, onMessagingClick: () -> Unit, onSustainabilityClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { MoreMenuItem("General info & property status", Icons.Default.Info, onGeneralInfoClick) }
        item { MoreMenuItem("VAT/Tax/Charges", Icons.Default.Payments, onVatTaxClick) }
        item { MoreMenuItem("Photos", Icons.Default.PhotoLibrary, onPhotosClick) }
        item { MoreMenuItem("Room details", Icons.Default.Bed, onRoomDetailsClick) }
        item { MoreMenuItem("Room Amenities", Icons.Default.HotTub, onAmenitiesClick) }
        item { MoreMenuItem("Your Profile", Icons.Default.AccountBox, onProfileClick) }
        item { MoreMenuItem("Messaging Preferences", Icons.Default.Message, onMessagingClick) }
    }
}
