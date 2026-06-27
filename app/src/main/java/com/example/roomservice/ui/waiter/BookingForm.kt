package com.example.roomservice.ui.waiter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.roomservice.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookingDialog(
    rooms: List<Room>,
    initialBooking: Booking? = null,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // FORM STATE
    var guestName by remember { mutableStateOf(initialBooking?.guestName ?: "") }
    var guestPhone by remember { mutableStateOf(initialBooking?.guestPhone ?: "") }
    var adults by remember { mutableIntStateOf(initialBooking?.numberOfGuests ?: 2) }
    var children by remember { mutableIntStateOf(0) }
    
    var checkInDate by remember { mutableLongStateOf(initialBooking?.checkInDate ?: System.currentTimeMillis()) }
    var checkOutDate by remember { mutableLongStateOf(initialBooking?.checkOutDate ?: (System.currentTimeMillis() + 86400000L)) }
    
    var roomRent by remember { mutableStateOf("") }
    var advancePaid by remember { mutableStateOf(initialBooking?.advancePaid?.toInt()?.toString() ?: "") }
    var paymentMode by remember { mutableStateOf("UPI") }
    
    var idType by remember { mutableStateOf("Aadhar Card") }
    var idNumber by remember { mutableStateOf("") }
    
    var specialRequests by remember { mutableStateOf("") }
    
    val agents = remember { listOf("Website Booking", "Individual Bookings", "booking.com", "Agoda", "Airbnb", "Goibibo", "yatra.com", "trivago", "Clear Trip", "Make My Trip") }
    var selectedAgent by remember { mutableStateOf(initialBooking?.bookingAgent ?: "Website Booking") }
    
    var selectedType by remember { mutableStateOf(initialBooking?.roomNumber?.let { rn -> rooms.find { it.roomNumber == rn }?.roomType } ?: if(rooms.isNotEmpty()) rooms[0].roomType else "") }

    // APP STATE
    var isSaving by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // ID PHOTO STATE
    var selectedIdUri by remember { mutableStateOf<Uri?>(null) }
    val cameraImageUri = remember { try { val file = File(context.cacheDir, "temp_id.jpg"); FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) } catch (e: Exception) { null } }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if (it) selectedIdUri = cameraImageUri }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) selectedIdUri = it }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it && cameraImageUri != null) cameraLauncher.launch(cameraImageUri) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    // DATE PICKERS
    var showCheckInPicker by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }

    // CALCULATIONS
    val nights by remember(checkInDate, checkOutDate) {
        derivedStateOf {
            val start = Calendar.getInstance().apply { timeInMillis = checkInDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val end = Calendar.getInstance().apply { timeInMillis = checkOutDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val diff = ((end.timeInMillis - start.timeInMillis) / 86400000L).toInt()
            if (diff <= 0) 1 else diff
        }
    }
    
    val pricePerNight = roomRent.toDoubleOrNull() ?: 0.0
    val totalAmount = pricePerNight * nights
    val advanceVal = advancePaid.toDoubleOrNull() ?: 0.0
    val remaining = totalAmount - advanceVal

    // UI STATE
    var expandedSection by remember { mutableStateOf("guest") }
    val hotelId by com.example.roomservice.data.HotelSession.hotelId.collectAsState()
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (initialBooking == null) "New Manual Booking" else "Edit Booking", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                delay(1000) // Feedback delay
                                val assignedRoom = rooms.find { it.roomType == selectedType }?.roomNumber ?: ""
                                onConfirm(Booking(
                                    id = initialBooking?.id ?: UUID.randomUUID().toString(),
                                    hotelId = hotelId ?: "",
                                    roomNumber = assignedRoom,
                                    guestName = guestName,
                                    guestPhone = guestPhone,
                                    checkInDate = checkInDate,
                                    checkOutDate = checkOutDate,
                                    totalAmount = totalAmount,
                                    advancePaid = advanceVal,
                                    numberOfGuests = adults + children,
                                    status = initialBooking?.status ?: BookingStatus.BOOKED,
                                    bookingAgent = selectedAgent
                                ))
                                isSaving = false
                                showSaveDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        enabled = !isSaving && guestName.isNotBlank() && guestPhone.isNotBlank() && selectedType.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save Booking", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F9FA)), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // GUEST DETAILS
                item {
                    BookingAccordionSection("Guest Details", Icons.Default.Person, expandedSection == "guest", { expandedSection = if(expandedSection == "guest") "" else "guest" }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = guestName, onValueChange = { guestName = it }, label = { Text("Guest Name *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                            OutlinedTextField(value = guestPhone, onValueChange = { guestPhone = it }, label = { Text("Mobile Number *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.Gray) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) { Text("Adults *", fontSize = 12.sp, fontWeight = FontWeight.Bold); SimpleCounter(adults) { adults = it } }
                                Column(modifier = Modifier.weight(1f)) { Text("Children", fontSize = 12.sp, fontWeight = FontWeight.Bold); SimpleCounter(children) { children = it } }
                            }
                        }
                    }
                }

                // STAY DETAILS
                item {
                    BookingAccordionSection("Stay Details", Icons.Default.CalendarMonth, expandedSection == "stay", { expandedSection = if(expandedSection == "stay") "" else "stay" }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = df.format(Date(checkInDate)), onValueChange = {}, readOnly = true, label = { Text("Check-in Date *") }, modifier = Modifier.fillMaxWidth().clickable { showCheckInPicker = true }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.LightGray, disabledLabelColor = Color.Gray), shape = RoundedCornerShape(8.dp), trailingIcon = { Icon(Icons.Default.CalendarToday, null) })
                            OutlinedTextField(value = df.format(Date(checkOutDate)), onValueChange = {}, readOnly = true, label = { Text("Check-out Date *") }, modifier = Modifier.fillMaxWidth().clickable { showCheckOutPicker = true }, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.LightGray, disabledLabelColor = Color.Gray), shape = RoundedCornerShape(8.dp), trailingIcon = { Icon(Icons.Default.CalendarToday, null) })
                            Box(modifier = Modifier.background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) { Text("$nights Nights", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                            val roomTypeOptions = remember(rooms) { rooms.map { it.roomType }.distinct() }
                            SimpleDropdown("Room Type *", roomTypeOptions, selectedType) { selectedType = it }
                            SimpleDropdown("Booking Agent", agents, selectedAgent) { selectedAgent = it }
                        }
                    }
                }

                // PAYMENT DETAILS
                item {
                    BookingAccordionSection("Payment Details", Icons.Default.Wallet, expandedSection == "payment", { expandedSection = if(expandedSection == "payment") "" else "payment" }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = roomRent, onValueChange = { roomRent = it }, label = { Text("Room Rent / Night *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), prefix = { Text("₹ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = totalAmount.toInt().toString(), onValueChange = {}, readOnly = true, label = { Text("Total Amount") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), prefix = { Text("₹ ") })
                            OutlinedTextField(value = advancePaid, onValueChange = { advancePaid = it }, label = { Text("Advance Paid") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), prefix = { Text("₹ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF9C4), RoundedCornerShape(8.dp)).padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Remaining Amount", fontWeight = FontWeight.Medium); Text("₹ ${remaining.toInt()}", fontWeight = FontWeight.Bold) } }
                            SimpleDropdown("Payment Mode *", listOf("UPI", "Cash", "Card", "Net Banking"), paymentMode) { paymentMode = it }
                        }
                    }
                }

                // ID PROOF
                item {
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
                }

                // SPECIAL REQUESTS
                item {
                    BookingAccordionSection("Special Requests / Notes", Icons.Default.EditNote, expandedSection == "notes", { expandedSection = if(expandedSection == "notes") "" else "notes" }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Special Requests / Remarks", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = specialRequests, onValueChange = { specialRequests = it }, placeholder = { Text("Enter any special request or note", color = Color.LightGray) }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(8.dp))
                        }
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }

        // FEEDBACK DIALOG
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false; onDismiss() },
                title = { Text("Success") },
                text = { Text("Booking has been saved successfully.") },
                confirmButton = { Button(onClick = { showSaveDialog = false; onDismiss() }) { Text("OK") } }
            )
        }

        // PICKERS
        if (showCheckInPicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = checkInDate)
            DatePickerDialog(onDismissRequest = { showCheckInPicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { checkInDate = it }; showCheckInPicker = false }) { Text("OK") } }) { DatePicker(state = state) }
        }
        if (showCheckOutPicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = checkOutDate)
            DatePickerDialog(onDismissRequest = { showCheckOutPicker = false }, confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { checkOutDate = it }; showCheckOutPicker = false }) { Text("OK") } }) { DatePicker(state = state) }
        }
        if (showPhotoOptions) {
            AlertDialog(onDismissRequest = { showPhotoOptions = false }, title = { Text("Select ID Photo") }, text = { Text("Choose a source for the photo") }, confirmButton = { TextButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraImageUri?.let { cameraLauncher.launch(it) } else permissionLauncher.launch(Manifest.permission.CAMERA); showPhotoOptions = false }) { Text("CAMERA") } }, dismissButton = { TextButton(onClick = { galleryLauncher.launch("image/*"); showPhotoOptions = false }) { Text("GALLERY") } })
        }
    }
}

@Composable
fun BookingAccordionSection(title: String, icon: ImageVector, isExpanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = if(isExpanded) Color(0xFF1976D2) else Color.Gray, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp)); Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if(isExpanded) Color(0xFF1976D2) else Color.Black)
                }
                Icon(if(isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
            }
            AnimatedVisibility(visible = isExpanded) { Column(modifier = Modifier.padding(top = 16.dp)) { content() } }
        }
    }
}

@Composable
fun SimpleCounter(value: Int, onValueChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if(value > 0) onValueChange(value - 1) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Remove, null) }
        Text(text = "$value", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        IconButton(onClick = { onValueChange(value + 1) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(8.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { options.forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false }) } }
    }
}
