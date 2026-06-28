package com.example.roomservice.ui.waiter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.roomservice.data.RoomRepository
import com.example.roomservice.data.HotelSession
import com.example.roomservice.data.model.Room
import com.example.roomservice.util.QRCodeGenerator
import com.example.roomservice.ui.util.zoomClick
import com.example.roomservice.util.SecurityManager
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomManagementScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val rooms by RoomRepository.rooms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRoomForDetails by remember { mutableStateOf<Room?>(null) }
    var selectedRoomForQR by remember { mutableStateOf<Room?>(null) }
    var roomToDelete by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Room Details", 
                modifier = Modifier.padding(16.dp), 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Individual Room Cards
                items(rooms) { room ->
                    val roomsOfTypeCount = rooms.count { it.roomType == room.roomType }
                    PropertyRoomCard(
                        room = room,
                        typeCount = roomsOfTypeCount,
                        onEdit = { selectedRoomForDetails = room },
                        onDelete = { roomToDelete = room.roomNumber },
                        onUpload = { /* Photo upload logic */ }
                    )
                }

                // Create New Room Card
                item {
                    CreateRoomCard(onClick = { showAddDialog = true })
                }
                
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        if (showAddDialog) AddEditRoomDialog(rooms, null, { showAddDialog = false }, { RoomRepository.addRoom(it) })
        if (selectedRoomForDetails != null) {
            AddEditRoomDialog(
                rooms = rooms, 
                initialRoom = selectedRoomForDetails, 
                onDismiss = { selectedRoomForDetails = null }, 
                onConfirm = { 
                    RoomRepository.updateRoom(it)
                    selectedRoomForDetails = null 
                },
                onDeleteClick = {
                    roomToDelete = selectedRoomForDetails!!.roomNumber
                    selectedRoomForDetails = null
                }
            )
        }
        if (selectedRoomForQR != null) QRDialog(selectedRoomForQR!!, { selectedRoomForQR = null }, { bitmap -> shareQRCode(context, bitmap, selectedRoomForQR!!.roomNumber) })
        if (roomToDelete != null) {
            AlertDialog(
                onDismissRequest = { roomToDelete = null },
                title = { Text("Delete Room") },
                text = { Text("Are you sure you want to delete room $roomToDelete?") },
                confirmButton = {
                    Button(onClick = {
                        RoomRepository.deleteRoom(roomToDelete!!)
                        roomToDelete = null
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("DELETE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { roomToDelete = null }) { Text("CANCEL") }
                }
            )
        }
    }
}

@Composable
fun PropertyRoomCard(
    room: Room,
    typeCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column {
            // Room Image with Header Overlay
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                val img = if (room.imageUrl.isNotEmpty()) room.imageUrl 
                          else "https://img.freepik.com/free-vector/interior-hotel-room-with-bed-window-sketch_107791-3048.jpg"
                
                AsyncImage(
                    model = img, 
                    contentDescription = null, 
                    modifier = Modifier.fillMaxSize(), 
                    contentScale = ContentScale.Crop
                )
                
                // Overlay text at the bottom of the image
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = room.roomType,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Total Units: ${room.totalUnits}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Room Details
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatLine("Maximum guests:", "${room.maxGuests} guests")
                StatLine("Maximum adults:", "${room.maxAdults} adults")
                StatLine("Maximum children:", "${room.maxChildren} children")
                StatLine("Total Units:", "${room.totalUnits}")

                Spacer(Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("Edit", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("Delete", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onUpload,
                        modifier = Modifier.weight(1.5f).height(40.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Upload photos", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateRoomCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF335C91)), // Dark blue
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Create a new room",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF335C91),
                    modifier = Modifier.padding(12.dp).size(32.dp)
                )
            }
        }
    }
}

@Composable
fun StatLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.weight(1.2f))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRoomDialog(
    rooms: List<Room>, 
    initialRoom: Room?, 
    onDismiss: () -> Unit, 
    onConfirm: (Room) -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    var roomType by remember { mutableStateOf(initialRoom?.roomType ?: "") }
    var totalUnits by remember { mutableIntStateOf(initialRoom?.totalUnits ?: 1) }
    var smokingPolicy by remember { mutableStateOf(initialRoom?.smokingPolicy ?: "I have both smoking and non-smoking options for this room type") }
    var floorLevel by remember { mutableStateOf(initialRoom?.floorLevel ?: "No selection") }
    var bedType by remember { mutableStateOf(initialRoom?.bedType ?: "") }
    var numberOfBeds by remember { mutableIntStateOf(initialRoom?.numberOfBeds ?: 1) }
    var maxGuests by remember { mutableIntStateOf(initialRoom?.maxGuests ?: 2) }
    var maxAdults by remember { mutableIntStateOf(initialRoom?.maxAdults ?: 2) }
    var maxChildren by remember { mutableIntStateOf(initialRoom?.maxChildren ?: 0) }
    var numBathrooms by remember { mutableIntStateOf(initialRoom?.numBathrooms ?: 1) }
    var isBathroomPrivate by remember { mutableStateOf(initialRoom?.isBathroomPrivate ?: true) }
    
    val hotelId by HotelSession.hotelId.collectAsState()

    val isDuplicateRoom = remember(roomNumber, roomType, rooms) {
        rooms.any { it.roomNumber == roomNumber && it.roomType == roomType && it.roomNumber != initialRoom?.roomNumber }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF1F5F9)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(if (initialRoom == null) "Room Details" else "Edit Room", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Please Select Section
                    item {
                        FormSectionCard("Room Configuration") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                DropdownField("Room type", listOf("Single", "Double", "Deluxe", "Suite", "Budget Double Room"), roomType) { roomType = it }
                                
                                OccupancyCounter("Number of rooms (of this type):", totalUnits) { totalUnits = it }
                                
                                DropdownField("Smoking policy", listOf("Non-smoking", "Smoking allowed", "I have both smoking and non-smoking options for this room type"), smokingPolicy) { smokingPolicy = it }

                                if (isDuplicateRoom) {
                                    Surface(
                                        color = Color(0xFFFFF9C4),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                Icons.Default.PanTool,
                                                contentDescription = null,
                                                tint = Color(0xFFFBC02D),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = "Please select a room name you haven't used yet. This will make it easier for you to differentiate between rooms when assigning amenities in the next section.",
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Room Location
                    item {
                        FormSectionCard("Room location") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DropdownField("Floor Level", listOf("Ground floor", "1st floor", "2nd floor", "3rd floor", "Basement", "No selection"), floorLevel) { floorLevel = it }
                                Text("The location will help your guests understand where the option is located on the property.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // 3. Bed Options
                    item {
                        FormSectionCard("Bed options") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Standard Arrangement", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF007BFF))
                                Text("What kind of beds are available in this room?", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(1.5f)) {
                                        DropdownField("Bed type", listOf("Single", "Double", "Queen", "King", "Twin"), bedType) { bedType = it }
                                    }
                                    Text("x", fontWeight = FontWeight.Bold)
                                    Box(modifier = Modifier.weight(1f)) {
                                        DropdownField("No. of beds", (1..5).map { it.toString() }, numberOfBeds.toString()) { numberOfBeds = it.toInt() }
                                    }
                                }

                                TextButton(onClick = { }, contentPadding = PaddingValues(0.dp)) {
                                    Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add another bed", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // 4. Occupancy
                    item {
                        FormSectionCard("Occupancy") {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("You can adjust your occupancy later. To attract more family bookings, make sure you set children rates and add extra beds.", fontSize = 11.sp, color = Color.Gray)
                                
                                OccupancyCounter("Maximum guests:", maxGuests) { maxGuests = it }
                                OccupancyCounter("Maximum adults:", maxAdults) { maxAdults = it }
                                OccupancyCounter("Maximum children:", maxChildren) { maxChildren = it }
                            }
                        }
                    }

                    // 5. Bathroom Options
                    item {
                        FormSectionCard("Bathroom options") {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                DropdownField("Number of bathrooms", (1..3).map { it.toString() }, numBathrooms.toString()) { numBathrooms = it.toInt() }
                                
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Bathroom 1", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Is the bathroom private? (not shared with host or other guests)", fontSize = 12.sp, color = Color.Gray)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = isBathroomPrivate, onClick = { isBathroomPrivate = true })
                                            Text("Yes", fontSize = 14.sp)
                                            Spacer(Modifier.width(16.dp))
                                            RadioButton(selected = !isBathroomPrivate, onClick = { isBathroomPrivate = false })
                                            Text("No", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Action Button
                    item {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                if (roomType.isNotBlank()) {
                                    onConfirm(Room(
                                        roomNumber = roomType, // Use Type as ID for now
                                        totalUnits = totalUnits,
                                        hotelId = hotelId ?: "", 
                                        roomType = roomType, 
                                        smokingPolicy = smokingPolicy, 
                                        floorLevel = floorLevel, 
                                        bedType = bedType, 
                                        numberOfBeds = numberOfBeds, 
                                        maxGuests = maxGuests, 
                                        maxAdults = maxAdults, 
                                        maxChildren = maxChildren, 
                                        numBathrooms = numBathrooms, 
                                        isBathroomPrivate = isBathroomPrivate
                                    ))
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                        ) {
                            Text("Continue", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FormSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun OccupancyCounter(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(
                onClick = { if (value > 0) onValueChange(value - 1) },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
            }
            Text(text = value.toString(), modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedIconButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(exp, { exp = !exp }, Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = if (selected.isEmpty()) "Please select" else selected, 
            onValueChange = {}, 
            readOnly = true, 
            label = { Text(label) }, 
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) }, 
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007BFF),
                unfocusedBorderColor = Color.LightGray
            )
        )
        ExposedDropdownMenu(exp, { exp = false }) { 
            options.forEach { 
                DropdownMenuItem(
                    text = { Text(it) }, 
                    onClick = { onSelect(it); exp = false }
                ) 
            } 
        }
    }
}

private fun shareQRCode(context: Context, bitmap: Bitmap, roomNumber: String) {
    try {
        val path = File(context.cacheDir, "images"); path.mkdirs()
        val file = File(path, "room_$roomNumber.png")
        val stream = FileOutputStream(file); bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); stream.close()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); setDataAndType(uri, context.contentResolver.getType(uri)); putExtra(Intent.EXTRA_STREAM, uri) }, "Share Room $roomNumber QR"))
    } catch (e: Exception) { e.printStackTrace() }
}

@Composable
fun QRDialog(room: Room, onDismiss: () -> Unit, onShare: (Bitmap) -> Unit) {
    val qrBitmap = remember(room) { 
        QRCodeGenerator.generate(room.generateWebPortalLink()) 
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("R-${room.roomNumber}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Scan to open app", color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                qrBitmap?.let { Image(it.asImageBitmap(), null, Modifier.size(200.dp)) }
                Spacer(modifier = Modifier.height(24.dp))
                Button({ qrBitmap?.let { onShare(it) } }, Modifier.fillMaxWidth()) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp)); Text("Share or Print") }
                TextButton(onDismiss, Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
