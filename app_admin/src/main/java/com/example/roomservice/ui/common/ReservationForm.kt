package com.example.roomservice.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.roomservice.data.model.*
import com.example.roomservice.ui.waiter.BookingAccordionSection
import com.example.roomservice.ui.waiter.SimpleCounter
import com.example.roomservice.ui.waiter.SimpleDropdown
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable Reservation Form Content
 * This component contains only the form fields and logic, making it easy to embed anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationFormContent(
    rooms: List<Room>,
    allBookings: List<Booking>,
    initialBooking: Booking? = null,
    onDataChanged: (Booking) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val hotelId by com.example.roomservice.data.HotelSession.hotelId.collectAsState()

    // --- FORM STATE ---
    var guestName by remember { mutableStateOf(initialBooking?.guestName ?: "") }
    var guestPhone by remember { mutableStateOf(initialBooking?.guestPhone ?: "") }
    var bookingDate by remember { mutableLongStateOf(initialBooking?.timestamp ?: System.currentTimeMillis()) }
    var checkInDate by remember { mutableLongStateOf(initialBooking?.checkInDate ?: DateRangeUtils.getNoonTimestamp(System.currentTimeMillis())) }
    var checkOutDate by remember { mutableLongStateOf(initialBooking?.checkOutDate ?: (DateRangeUtils.getNoonTimestamp(System.currentTimeMillis()) + 86400000L)) }
    
    // --- AVAILABILITY LOGIC ---
    val availabilityMap = remember(checkInDate, checkOutDate, allBookings, rooms, initialBooking) {
        val map = mutableMapOf<String, Int>()
        val distinctTypes = rooms.map { it.roomType }.distinct()
        distinctTypes.forEach { type ->
            val totalRoomsOfType = rooms.count { it.roomType == type }
            val overlappingBookings = allBookings.filter { b ->
                if (b.status == BookingStatus.CANCELLED || b.id == initialBooking?.id) return@filter false
                val bRoom = rooms.find { it.roomNumber == b.roomNumber }
                bRoom?.roomType == type && b.checkInDate < checkOutDate && b.checkOutDate > checkInDate
            }
            map[type] = (totalRoomsOfType - overlappingBookings.size).coerceAtLeast(0)
        }
        map
    }

    var selectedType by remember { 
        mutableStateOf(
            initialBooking?.roomNumber?.let { rn -> rooms.find { it.roomNumber == rn }?.roomType } 
            ?: availabilityMap.entries.find { it.value > 0 }?.key ?: if(rooms.isNotEmpty()) rooms[0].roomType else ""
        ) 
    }
    
    var roomQuantity by remember { mutableIntStateOf(initialBooking?.roomQuantity ?: 1) }
    
    val selectedRoom = remember(selectedType, rooms) { rooms.find { it.roomType == selectedType } }
    val maxAdultsLimit = selectedRoom?.maxAdults ?: 5
    val maxChildrenLimit = selectedRoom?.maxChildren ?: 5
    val maxTotalLimit = selectedRoom?.maxGuests ?: 10

    var adults by remember { mutableIntStateOf(initialBooking?.numberOfGuests?.coerceAtMost(maxAdultsLimit) ?: 2) }
    var children by remember { mutableIntStateOf(0) }
    var roomRent by remember { mutableStateOf(initialBooking?.roomRent?.toInt()?.toString() ?: "") }
    var advancePaid by remember { mutableStateOf(initialBooking?.advancePaid?.toInt()?.toString() ?: "") }
    var discount by remember { mutableStateOf(initialBooking?.discount?.toInt()?.toString() ?: "") }
    var paymentMode by remember { mutableStateOf(initialBooking?.paymentMode ?: "UPI") }
    
    var idType by remember { mutableStateOf(initialBooking?.guestIdentities?.firstOrNull()?.idType ?: "Aadhar Card") }
    var idNumber by remember { mutableStateOf(initialBooking?.guestIdentities?.firstOrNull()?.idNumber ?: "") }
    var specialRequests by remember { mutableStateOf("") }
    var selectedAgent by remember { mutableStateOf(initialBooking?.bookingAgent ?: "Manual Reservation") }

    // ID PHOTO STATE
    var selectedIdUri by remember { mutableStateOf<Uri?>(null) }
    val cameraImageUri = remember { try { val file = File(context.cacheDir, "temp_id.jpg"); FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) } catch (e: Exception) { null } }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if (it) selectedIdUri = cameraImageUri }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) selectedIdUri = it }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it && cameraImageUri != null) cameraLauncher.launch(cameraImageUri) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    // --- CALCULATIONS ---
    val nights by remember(checkInDate, checkOutDate) {
        derivedStateOf {
            val diff = ((checkOutDate - checkInDate) / 86400000L).toInt()
            if (diff <= 0) 1 else diff
        }
    }
    val pricePerNight = roomRent.toDoubleOrNull() ?: 0.0
    val discountVal = discount.toDoubleOrNull() ?: 0.0
    val totalAmount = (pricePerNight * nights * roomQuantity) - discountVal
    val advanceVal = (advancePaid.toDoubleOrNull() ?: 0.0)
    val remaining = totalAmount - advanceVal

    // --- UI STATE ---
    var expandedSection by remember { mutableStateOf("stay") }
    var showRangePicker by remember { mutableStateOf(false) }
    var showBookingDatePicker by remember { mutableStateOf(false) }

    // Notify parent on any change
    LaunchedEffect(guestName, guestPhone, bookingDate, checkInDate, checkOutDate, selectedType, roomQuantity, adults, children, roomRent, advancePaid, discount, paymentMode, idType, idNumber, specialRequests, selectedAgent) {
        val bookedRoomNumbers = allBookings.filter { b ->
            if (b.status == BookingStatus.CANCELLED || b.id == initialBooking?.id) return@filter false
            b.checkInDate < checkOutDate && b.checkOutDate > checkInDate
        }.map { it.roomNumber }
        
        val assignedRoom = rooms.find { it.roomType == selectedType && it.roomNumber !in bookedRoomNumbers }?.roomNumber ?: ""

        onDataChanged(Booking(
            id = initialBooking?.id ?: "",
            bookingNumber = initialBooking?.bookingNumber ?: "",
            hotelId = hotelId ?: "",
            roomNumber = assignedRoom,
            roomType = selectedType,
            roomQuantity = roomQuantity,
            guestName = guestName,
            guestPhone = guestPhone,
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            roomRent = pricePerNight,
            totalAmount = totalAmount,
            advancePaid = advanceVal,
            paymentMode = paymentMode,
            discount = discountVal,
            isFullPay = false,
            numberOfGuests = adults + children,
            guestIdentities = listOf(GuestIdentity(idType = idType, idNumber = idNumber)),
            status = initialBooking?.status ?: BookingStatus.BOOKED,
            bookingAgent = selectedAgent,
            timestamp = bookingDate
        ))
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // BOOKING DATE (Editable Card with Shadow)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBookingDatePicker = true },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Icon(
                        Icons.Default.Event, 
                        contentDescription = null, 
                        tint = Color(0xFF1976D2), 
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Booking Date (Click to Change)", 
                        fontSize = 12.sp, 
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = df.format(Date(bookingDate)),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF1E293B)
                    )
                }
                Icon(Icons.Default.Edit, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
        }

        // STAY DETAILS
        BookingAccordionSection("Stay Details", Icons.Default.CalendarMonth, expandedSection == "stay", { expandedSection = if(expandedSection == "stay") "" else "stay" }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateDisplayBox("Check-in *", df.format(Date(checkInDate)), { showRangePicker = true }, Modifier.weight(1f))
                    DateDisplayBox("Check-out *", df.format(Date(checkOutDate)), { showRangePicker = true }, Modifier.weight(1f))
                }
                Box(Modifier.background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) { Text("$nights Nights", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                
                // Room Type Dropdown with Availability
                var dropExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = dropExpanded, onExpandedChange = { dropExpanded = !dropExpanded }) {
                    OutlinedTextField(
                        value = if(selectedType.isEmpty()) "Select Room Type" else "$selectedType (${availabilityMap[selectedType] ?: 0} left)",
                        onValueChange = {}, readOnly = true, label = { Text("Room Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = dropExpanded, onDismissRequest = { dropExpanded = false }) {
                        rooms.map { it.roomType }.distinct().forEach { type ->
                            val count = availabilityMap[type] ?: 0
                            val isEnabled = count > 0
                            DropdownMenuItem(
                                text = { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(type); Text(if(isEnabled) "$count available" else "Sold Out", color = if(isEnabled) Color(0xFF2E7D32) else Color.Red, fontSize = 11.sp) } },
                                onClick = { if(isEnabled) { selectedType = type; dropExpanded = false } },
                                enabled = isEnabled, modifier = Modifier.alpha(if(isEnabled) 1f else 0.5f)
                            )
                        }
                    }
                }

                // Room Quantity (Unit) Selection
                val maxUnitsAvailable = availabilityMap[selectedType] ?: 1
                Column {
                    Text("Room Quantity (Units) *", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    SimpleCounter(
                        value = roomQuantity,
                        maxValue = maxUnitsAvailable,
                        onValueChange = { roomQuantity = it }
                    )
                    if (maxUnitsAvailable > 0) {
                        Text("Available for selected dates: $maxUnitsAvailable", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                // ADULTS & CHILDREN MOVED HERE
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) { Text("Adults", fontSize = 12.sp, fontWeight = FontWeight.Bold); SimpleCounter(adults, maxAdultsLimit) { adults = it } }
                    Column(Modifier.weight(1f)) { Text("Children", fontSize = 12.sp, fontWeight = FontWeight.Bold); SimpleCounter(children, maxChildrenLimit) { children = it } }
                }
            }
        }

        // GUEST DETAILS
        BookingAccordionSection("Guest Details", Icons.Default.Person, expandedSection == "guest", { expandedSection = if(expandedSection == "guest") "" else "guest" }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = guestName, onValueChange = { guestName = it }, label = { Text("Guest Name *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = guestPhone, onValueChange = { guestPhone = it }, label = { Text("Mobile Number *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            }
        }

        // PAYMENT DETAILS
        BookingAccordionSection("Payment Details", Icons.Default.Wallet, expandedSection == "payment", { expandedSection = if(expandedSection == "payment") "" else "payment" }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = roomRent, onValueChange = { roomRent = it }, label = { Text("Room Rent / Night *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), prefix = { Text("₹ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Discount") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), prefix = { Text("₹ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = advancePaid, onValueChange = { advancePaid = it }, label = { Text("Advance Paid") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), prefix = { Text("₹ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                
                Box(Modifier.fillMaxWidth().background(Color(0xFFFFF9C4), RoundedCornerShape(8.dp)).padding(16.dp)) {
                    Column {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Total Amount"); Text("₹ ${totalAmount.toInt()}", fontWeight = FontWeight.Bold) }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Remaining"); Text("₹ ${remaining.toInt()}", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // ID PROOF
        BookingAccordionSection("ID Proof", Icons.Default.Badge, expandedSection == "id", { expandedSection = if(expandedSection == "id") "" else "id" }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SimpleDropdown("ID Type *", listOf("Aadhar Card", "PAN Card", "Passport", "Voter ID"), idType) { idType = it }
                OutlinedTextField(value = idNumber, onValueChange = { idNumber = it }, label = { Text("ID Number *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).background(Color.White).clickable { showPhotoOptions = true }, contentAlignment = Alignment.Center) {
                    if (selectedIdUri != null) { AsyncImage(model = selectedIdUri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop) }
                    else { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.CloudUpload, null, tint = Color(0xFF1976D2), modifier = Modifier.size(32.dp)); Text("Tap to upload ID Photo", color = Color(0xFF1976D2), fontSize = 14.sp) } }
                }
            }
        }

        // SPECIAL REQUESTS
        BookingAccordionSection("Special Requests / Notes", Icons.Default.EditNote, expandedSection == "notes", { expandedSection = if(expandedSection == "notes") "" else "notes" }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Special Requests / Remarks", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = specialRequests, onValueChange = { specialRequests = it }, placeholder = { Text("Enter any special request or note", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(8.dp))
            }
        }
    }

    // RANGE PICKER DIALOG
    if (showRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(initialSelectedStartDateMillis = checkInDate, initialSelectedEndDateMillis = checkOutDate)
        com.example.roomservice.ui.common.CommonDateRangePicker(state = dateRangePickerState, onDismiss = { showRangePicker = false }, onConfirm = { start, end -> 
            start?.let { checkInDate = DateRangeUtils.getNoonTimestamp(it) }
            end?.let { checkOutDate = DateRangeUtils.getNoonTimestamp(it) }
        })
    }

    if (showPhotoOptions) {
        AlertDialog(onDismissRequest = { showPhotoOptions = false }, title = { Text("Select ID Photo") }, text = { Text("Choose a source for the photo") }, confirmButton = { TextButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraImageUri?.let { cameraLauncher.launch(it) } else permissionLauncher.launch(Manifest.permission.CAMERA); showPhotoOptions = false }) { Text("CAMERA") } }, dismissButton = { TextButton(onClick = { galleryLauncher.launch("image/*"); showPhotoOptions = false }) { Text("GALLERY") } })
    }

    if (showBookingDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = bookingDate)
        DatePickerDialog(
            onDismissRequest = { showBookingDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { bookingDate = it }
                    showBookingDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showBookingDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
