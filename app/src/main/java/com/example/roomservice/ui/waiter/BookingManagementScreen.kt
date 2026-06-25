package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.GuestIdentity
import com.example.roomservice.data.model.BookingStatus
import com.example.roomservice.data.model.Room
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingManagementScreen(
    bookings: List<Booking>,
    rooms: List<Room>,
    onBackClick: () -> Unit,
    onAddBooking: (Booking) -> Unit,
    onDeleteBooking: (String) -> Unit,
    onUpdateStatus: (String, BookingStatus) -> Unit,
    onCheckInWithId: (String, String) -> Unit = { _, _ -> }
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            TopAppBar(
                title = { Text("Bookings", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (rooms.isEmpty()) {
                            // Show toast or alert
                        } else {
                            showAddDialog = true 
                        }
                    }) {
                        Icon(Icons.Default.Add, "New Booking", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (rooms.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BedroomParent, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("Please create rooms first in 'Rooms' section.", color = Color.Gray)
                }
            }
        } else if (bookings.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No manual bookings recorded.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF1F5F9)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings.sortedByDescending { it.checkInDate }) { booking ->
                    BookingCard(booking, rooms, onDeleteBooking, onUpdateStatus, onCheckInWithId)
                }
            }
        }

        if (showAddDialog) {
            AddBookingDialog(
                rooms = rooms,
                onDismiss = { showAddDialog = false },
                onConfirm = { 
                    onAddBooking(it)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    rooms: List<Room>,
    onDelete: (String) -> Unit,
    onUpdateStatus: (String, BookingStatus) -> Unit,
    onCheckInWithId: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val df = SimpleDateFormat("dd MMM", Locale.getDefault())
    val checkInStr = df.format(Date(booking.checkInDate))
    val checkOutStr = df.format(Date(booking.checkOutDate))
    
    val roomType = remember(booking.roomNumber, rooms) {
        rooms.find { it.roomNumber == booking.roomNumber }?.roomType ?: ""
    }

    val outstanding = booking.totalAmount - booking.advancePaid

    var guestIdentities by remember(booking.id, booking.guestIdentities) { 
        mutableStateOf(if (booking.guestIdentities.isNotEmpty()) booking.guestIdentities 
                      else List(booking.numberOfGuests) { GuestIdentity() }) 
    }
    
    fun updateGuestIdInfo(index: Int, type: String? = null, number: String? = null) {
        val newList = guestIdentities.toMutableList()
        val current = newList[index]
        newList[index] = current.copy(
            idType = type ?: current.idType,
            idNumber = number ?: current.idNumber
        )
        guestIdentities = newList
    }

    // Helper to update a specific photo
    fun updateGuestPhoto(index: Int, isFront: Boolean, uri: Uri) {
        val newList = guestIdentities.toMutableList()
        val current = newList[index]
        newList[index] = if (isFront) current.copy(frontPhotoUrl = uri.toString()) 
                         else current.copy(backPhotoUrl = uri.toString())
        guestIdentities = newList
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = booking.guestName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
                    
                    if (booking.guestPhone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = booking.guestPhone, fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                data = android.net.Uri.parse("tel:${booking.guestPhone}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    Text(text = "$checkInStr - $checkOutStr", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text("Total", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("₹${booking.totalAmount}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Paid", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("₹${booking.advancePaid}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("Pending", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("₹$outstanding", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(booking.status)
                    Spacer(Modifier.height(8.dp))
                    Text(text = "Room ${booking.roomNumber}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    if (roomType.isNotEmpty()) {
                        Text(text = roomType, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

            Column(modifier = Modifier.padding(16.dp)) {
                // ID Photo Section (For all guests)
                if (booking.status == BookingStatus.BOOKED) {
                    Text("Guest Identification", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    
                    val idTypes = listOf("Aadhaar Card", "Voter ID", "Driving License", "Passport")

                    guestIdentities.forEachIndexed { index, guest ->
                        Column(modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                            Text("Guest ${index + 1} Details", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            
                            var expanded by remember { mutableStateOf(false) }
                            
                            Box(modifier = Modifier.padding(top = 8.dp)) {
                                OutlinedTextField(
                                    value = guest.idType ?: "",
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("ID Type") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { 
                                        IconButton(onClick = { expanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, null)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    idTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                updateGuestIdInfo(index, type = type)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = guest.idNumber ?: "",
                                onValueChange = { updateGuestIdInfo(index, number = it) },
                                label = { Text("ID Number") },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IdentityCaptureBox(label = "FRONT ID", currentUrl = guest.frontPhotoUrl, onCapture = { uri -> updateGuestPhoto(index, true, uri) }, modifier = Modifier.weight(1f))
                                IdentityCaptureBox(label = "BACK ID", currentUrl = guest.backPhotoUrl, onCapture = { uri -> updateGuestPhoto(index, false, uri) }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else if (booking.guestIdentities.isNotEmpty()) {
                    Text("Verified Guest Identities:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    booking.guestIdentities.forEachIndexed { index, guest ->
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text("Guest ${index + 1}: ${guest.idType} - ${guest.idNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AsyncImage(model = guest.frontPhotoUrl, contentDescription = null, modifier = Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                                AsyncImage(model = guest.backPhotoUrl, contentDescription = null, modifier = Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onDelete(booking.id) }) {
                        Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.6f))
                    }
                    
                    if (booking.status == BookingStatus.BOOKED) {
                        val allIdsCaptured = guestIdentities.all { 
                            !it.idType.isNullOrBlank() && !it.idNumber.isNullOrBlank() && it.frontPhotoUrl != null 
                        }
                        Button(
                            onClick = { 
                                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("hotels")
                                    .child(booking.hotelId).child("bookings").child(booking.id)
                                    .child("guestIdentities").setValue(guestIdentities).addOnSuccessListener {
                                        onUpdateStatus(booking.id, BookingStatus.CHECKED_IN)
                                    }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = allIdsCaptured
                        ) {
                            Text("CHECK IN", fontWeight = FontWeight.Bold)
                        }
                    } else if (booking.status == BookingStatus.CHECKED_IN) {
                        Button(
                            onClick = { onUpdateStatus(booking.id, BookingStatus.COMPLETED) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CHECK OUT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IdentityCaptureBox(
    label: String,
    currentUrl: String?,
    onCapture: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOptions by remember { mutableStateOf(false) }

    val cameraImageUri = remember { 
        try { 
            val file = File(context.cacheDir, "id_temp_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null } 
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) onCapture(cameraImageUri)
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onCapture(uri)
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted && cameraImageUri != null) cameraLauncher.launch(cameraImageUri)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .clickable { showOptions = true },
            contentAlignment = Alignment.Center
        ) {
            if (currentUrl != null) {
                AsyncImage(
                    model = currentUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("Select ID $label") },
            text = { Text("Choose a method to provide the identity document.") },
            confirmButton = {
                TextButton(onClick = { 
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraImageUri?.let { cameraLauncher.launch(it) }
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    showOptions = false 
                }) { Text("CAMERA") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    galleryLauncher.launch("image/*")
                    showOptions = false 
                }) { Text("GALLERY") }
            }
        )
    }
}

@Composable
fun StatusBadge(status: BookingStatus) {
    val color = when(status) {
        BookingStatus.BOOKED -> Color(0xFF1976D2)
        BookingStatus.CHECKED_IN -> Color(0xFFFFA000)
        BookingStatus.COMPLETED -> Color(0xFF2E7D32)
        BookingStatus.CANCELLED -> Color.Red
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text = status.name,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookingDialog(
    rooms: List<Room>,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit
) {
    var guestName by remember { mutableStateOf("") }
    var guestPhone by remember { mutableStateOf("") }
    
    val roomTypes = remember(rooms) { 
        listOf("All") + rooms.map { it.roomType }.distinct().sorted()
    }
    var selectedType by remember { mutableStateOf("All") }
    var typeExp by remember { mutableStateOf(false) }
    
    var selectedRoom by remember { mutableStateOf("") }
    var roomExp by remember { mutableStateOf(false) }

    var totalAmount by remember { mutableStateOf("") }
    var advancePaid by remember { mutableStateOf("") }
    var numberOfGuests by remember { mutableIntStateOf(1) }

    val roomsOfType = remember(selectedType, rooms) { 
        if (selectedType == "All") rooms 
        else rooms.filter { it.roomType.contains(selectedType, ignoreCase = true) } 
    }
    
    // Auto-select first room when list or type changes
    LaunchedEffect(roomsOfType) {
        if (roomsOfType.isNotEmpty()) {
            if (selectedRoom.isEmpty() || !roomsOfType.any { it.roomNumber == selectedRoom }) {
                selectedRoom = roomsOfType.first().roomNumber
            }
        } else {
            selectedRoom = ""
        }
    }

    var showRangePicker by remember { mutableStateOf(false) }
    var checkInDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var checkOutDate by remember { mutableLongStateOf(System.currentTimeMillis() + (24 * 60 * 60 * 1000L)) }

    val hotelId by com.example.roomservice.data.HotelSession.hotelId.collectAsState()
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    if (showRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = checkInDate,
            initialSelectedEndDateMillis = checkOutDate
        )
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateRangePickerState.selectedStartDateMillis?.let { checkInDate = it }
                    dateRangePickerState.selectedEndDateMillis?.let { checkOutDate = it }
                    showRangePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text("CANCEL") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f).padding(16.dp),
                title = { Text("Select Booking Dates", modifier = Modifier.padding(16.dp)) }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Booking", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(value = guestName, onValueChange = { guestName = it }, label = { Text("Guest Name") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = guestPhone, onValueChange = { guestPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Room Type Selection
                        ExposedDropdownMenuBox(
                            expanded = typeExp,
                            onExpandedChange = { typeExp = !typeExp },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Room Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExp) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = typeExp, onDismissRequest = { typeExp = false }) {
                                roomTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = { selectedType = type; typeExp = false }
                                    )
                                }
                            }
                        }

                        // Room Number Selection (Filtered by Type)
                        ExposedDropdownMenuBox(
                            expanded = roomExp,
                            onExpandedChange = { roomExp = !roomExp },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = if (selectedRoom.isEmpty()) "No Rooms" else "Room $selectedRoom",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Room No.") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roomExp) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = roomExp, onDismissRequest = { roomExp = false }) {
                                if (roomsOfType.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No rooms available") }, onClick = { roomExp = false })
                                } else {
                                    roomsOfType.forEach { room ->
                                        DropdownMenuItem(
                                            text = { Text("Room ${room.roomNumber}") },
                                            onClick = { selectedRoom = room.roomNumber; roomExp = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Surface(
                        onClick = { showRangePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.LightGray),
                        color = Color.White
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Check-In / Check-Out", fontSize = 10.sp, color = Color.Gray)
                                Text("${df.format(Date(checkInDate))} - ${df.format(Date(checkOutDate))}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            }
                            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Guests:", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (numberOfGuests > 1) numberOfGuests-- }) {
                                Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "$numberOfGuests",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { numberOfGuests++ }) {
                                Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = totalAmount, onValueChange = { totalAmount = it }, label = { Text("Total Bill") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = advancePaid, onValueChange = { advancePaid = it }, label = { Text("Advance") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bId = "BK-" + System.currentTimeMillis().toString().takeLast(6)
                    onConfirm(Booking(
                        id = UUID.randomUUID().toString(),
                        bookingNumber = bId,
                        hotelId = hotelId ?: "",
                        roomNumber = selectedRoom,
                        guestName = guestName,
                        guestPhone = guestPhone,
                        checkInDate = checkInDate,
                        checkOutDate = checkOutDate,
                        totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                        advancePaid = advancePaid.toDoubleOrNull() ?: 0.0,
                        numberOfGuests = numberOfGuests
                    ))
                },
                enabled = guestName.isNotBlank() && selectedRoom.isNotBlank() && checkOutDate > checkInDate
            ) {
                Text("SAVE BOOKING")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
