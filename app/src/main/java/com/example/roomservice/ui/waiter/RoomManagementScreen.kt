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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomManagementScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    val rooms by RoomRepository.rooms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRoomForDetails by remember { mutableStateOf<Room?>(null) }
    var selectedRoomForQR by remember { mutableStateOf<Room?>(null) }
    var roomToDelete by remember { mutableStateOf<String?>(null) }

    var showSecurityVerification by remember { mutableStateOf(false) }
    var onSecurityVerifiedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestSecureAction(action: () -> Unit) {
        onSecurityVerifiedAction = action
        showSecurityVerification = true
    }

    Scaffold(
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            TopAppBar(
                title = { Text("Rooms", color = Color.Black, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { requestSecureAction { showAddDialog = true } }) {
                        Icon(Icons.Default.Add, "Add Room", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Only showing List View as requested
            RoomListView(
                rooms = rooms, 
                onQR = { selectedRoomForQR = it }, 
                onDelete = { room -> requestSecureAction { roomToDelete = room.roomNumber } }, 
                onClick = { room -> selectedRoomForDetails = room }, // Opening spec dialog directly
                onToggleAvailability = { roomNum -> requestSecureAction { RoomRepository.toggleRoomAvailability(roomNum) } }
            )
        }

        if (showAddDialog) AddEditRoomDialog(rooms, null, { showAddDialog = false }, { RoomRepository.addRoom(it) })
        if (selectedRoomForDetails != null) {
            AddEditRoomDialog(
                rooms = rooms, 
                initialRoom = selectedRoomForDetails, 
                onDismiss = { selectedRoomForDetails = null }, 
                onConfirm = { 
                    requestSecureAction { 
                        RoomRepository.updateRoom(it)
                        selectedRoomForDetails = null 
                    }
                },
                onDeleteClick = {
                    requestSecureAction {
                        roomToDelete = selectedRoomForDetails!!.roomNumber
                        selectedRoomForDetails = null
                    }
                }
            )
        }
        if (selectedRoomForQR != null) QRDialog(selectedRoomForQR!!, { selectedRoomForQR = null }, { bitmap -> shareQRCode(context, bitmap, selectedRoomForQR!!.roomNumber) })
        if (roomToDelete != null) SecurityDeleteDialog(securityManager.getPin(), { roomToDelete = null }, { RoomRepository.deleteRoom(roomToDelete!!); roomToDelete = null; Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show() })

        if (showSecurityVerification) {
            SecurityVerificationDialog(
                onDismiss = { showSecurityVerification = false },
                onSuccess = {
                    showSecurityVerification = false
                    onSecurityVerifiedAction?.invoke()
                    onSecurityVerifiedAction = null
                },
                securityManager = securityManager
            )
        }
    }
}

@Composable
fun RoomGridViewSmall(rooms: List<Room>, onQR: (Room) -> Unit, onDelete: (Room) -> Unit, onClick: (Room) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rooms.size) { index ->
            val room = rooms[index]
            Card(onClick = { onClick(room) }, modifier = Modifier.aspectRatio(1f)) {
                Column(modifier = Modifier.fillMaxSize().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.size(6.dp).background(if (room.isAvailable) Color(0xFF00E676) else Color.Red, CircleShape))
                    Text("R-${room.roomNumber}", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun RoomListView(
    rooms: List<Room>, 
    onQR: (Room) -> Unit, 
    onDelete: (Room) -> Unit, 
    onClick: (Room) -> Unit,
    onToggleAvailability: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(rooms) { room -> 
            RoomCard(
                room = room, 
                onGenerateQR = { onQR(room) }, 
                onDelete = { onDelete(room) }, 
                onClick = { onClick(room) },
                onToggleAvailability = { onToggleAvailability(room.roomNumber) }
            ) 
        }
    }
}

@Composable
fun RoomDetailListView(rooms: List<Room>, onQR: (Room) -> Unit, onDelete: (Room) -> Unit, onClick: (Room) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(rooms) { room ->
            Card(onClick = { onClick(room) }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("R-${room.roomNumber}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = room.isAvailable, onCheckedChange = { RoomRepository.toggleRoomAvailability(room.roomNumber) }, modifier = Modifier.scale(0.7f), colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2E7D32), uncheckedThumbColor = Color.Red))
                    }
                    Text("${room.roomType} • ${room.bedType}", color = Color.Gray)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { onQR(room) }) { Icon(Icons.Default.QrCode, null) }
                        IconButton(onClick = { onDelete(room) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomSingleViewLarge(rooms: List<Room>, onQR: (Room) -> Unit, onDelete: (Room) -> Unit, onClick: (Room) -> Unit) {
    if (rooms.isEmpty()) return
    var currentIndex by remember { mutableIntStateOf(0) }
    val room = rooms[currentIndex]
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp)) {
            val img = if (room.imageUrl.isNotEmpty()) room.imageUrl else "https://img.freepik.com/free-vector/interior-hotel-room-with-bed-window-sketch_107791-3048.jpg"
            Column {
                AsyncImage(model = img, contentDescription = null, modifier = Modifier.fillMaxWidth().height(250.dp), contentScale = ContentScale.Crop)
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("R-${room.roomNumber}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                    Text(room.roomType, style = MaterialTheme.typography.headlineSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Policy: ${room.smokingPolicy}", fontSize = 14.sp)
                    Text("Beds: ${room.numberOfBeds} (${room.bedType})", fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onClick(room) }, modifier = Modifier.weight(1f)) { Text("EDIT ROOM") }
                        OutlinedButton(onClick = { onQR(room) }, modifier = Modifier.weight(1f)) { Text("QR CODE") }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { if (currentIndex > 0) currentIndex-- }) { Text("PREVIOUS") }
            TextButton(onClick = { if (currentIndex < rooms.size - 1) currentIndex++ }) { Text("NEXT") }
        }
    }
}

@Composable
fun SecurityDeleteDialog(savedPin: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Security Check") }, text = { Column { Text("Enter 6-digit MPIN to delete room."); Spacer(Modifier.height(16.dp)); OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6) { pin = it; error = false } }, visualTransformation = PasswordVisualTransformation(), isError = error, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { Button(onClick = { if (pin == savedPin) onConfirm() else error = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("DELETE") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
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
    val nextAvail = remember(rooms) { val used = rooms.mapNotNull { it.roomNumber.toIntOrNull() }.toSet(); var next = 1; while (used.contains(next)) next++; next.toString() }
    var roomNumber by remember { mutableStateOf(initialRoom?.roomNumber ?: nextAvail) }
    var roomType by remember { mutableStateOf(initialRoom?.roomType ?: "Deluxe Room") }
    var smokingPolicy by remember { mutableStateOf(initialRoom?.smokingPolicy ?: "Non-smoking") }
    var floorLevel by remember { mutableStateOf(initialRoom?.floorLevel ?: "Ground floor") }
    var bedType by remember { mutableStateOf(initialRoom?.bedType ?: "King Size") }
    var numberOfBeds by remember { mutableIntStateOf(initialRoom?.numberOfBeds ?: 1) }
    var maxGuests by remember { mutableIntStateOf(initialRoom?.maxGuests ?: 2) }
    var maxAdults by remember { mutableIntStateOf(initialRoom?.maxAdults ?: 2) }
    var maxChildren by remember { mutableIntStateOf(initialRoom?.maxChildren ?: 0) }
    var numBathrooms by remember { mutableIntStateOf(initialRoom?.numBathrooms ?: 1) }
    var isBathroomPrivate by remember { mutableStateOf(initialRoom?.isBathroomPrivate ?: true) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(if (initialRoom?.imageUrl?.isNotEmpty() == true) Uri.parse(initialRoom.imageUrl) else null) }
    val context = LocalContext.current
    val cameraImageUri = remember { try { val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg"); FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) } catch (e: Exception) { null } }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if (it) selectedImageUri = cameraImageUri }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) selectedImageUri = it }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it && cameraImageUri != null) cameraLauncher.launch(cameraImageUri) }
    val hotelId by HotelSession.hotelId.collectAsState()

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initialRoom == null) "New Room" else "Specifications") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { DropdownField("Room Type", listOf("Single", "Double", "Deluxe", "Suite"), roomType) { roomType = it } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(modifier = Modifier.weight(1f)) { DropdownField("Room No", (1..50).map { it.toString() }, roomNumber) { roomNumber = it } }; Box(modifier = Modifier.weight(1f)) { DropdownField("Smoking", listOf("Non-smoking", "Smoking allowed"), smokingPolicy) { smokingPolicy = it } } } }
            item { DropdownField("Floor", listOf("Ground", "1st", "2nd", "3rd"), floorLevel) { floorLevel = it } }
            item { Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.03f))) { Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { DropdownField("Bed Type", listOf("Single", "Double", "Queen", "King"), bedType) { bedType = it }; DropdownField("Beds", listOf("1", "2", "3"), numberOfBeds.toString()) { numberOfBeds = it.toInt() } } } }
            item { CounterField("Max Guests", maxGuests) { maxGuests = it }; CounterField("Adults", maxAdults) { maxAdults = it }; CounterField("Children", maxChildren) { maxChildren = it } }
            item { DropdownField("Bathrooms", listOf("1", "2"), numBathrooms.toString()) { numBathrooms = it.toInt() }; Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(isBathroomPrivate, { isBathroomPrivate = it }); Text("Private Bathroom") } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraImageUri?.let { cameraLauncher.launch(it) } else permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.weight(1f)) { Text("Camera", fontSize = 12.sp) }; Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("Gallery", fontSize = 12.sp) } } }
            if (selectedImageUri != null) item { AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop) }
            
            // Delete Option inside Specifications
            if (initialRoom != null && onDeleteClick != null) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("DELETE THIS ROOM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }, confirmButton = { Button(onClick = { if (roomNumber.isNotBlank()) { onConfirm(Room(roomNumber = roomNumber, hotelId = hotelId ?: "", roomType = roomType, smokingPolicy = smokingPolicy, floorLevel = floorLevel, bedType = bedType, numberOfBeds = numberOfBeds, maxGuests = maxGuests, maxAdults = maxAdults, maxChildren = maxChildren, numBathrooms = numBathrooms, isBathroomPrivate = isBathroomPrivate, imageUrl = selectedImageUri?.toString() ?: "")); onDismiss() } }) { Text("SAVE") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var exp by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(exp, { exp = !exp }, Modifier.fillMaxWidth()) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(exp, { exp = false }) { options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); exp = false }) } }
    }
}

@Composable
fun CounterField(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(onClick = { if (value > 0) onChange(value - 1) }, Modifier.size(32.dp)) { Icon(Icons.Default.Remove, null) }
            Text(value.toString(), Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
            OutlinedIconButton(onClick = { onChange(value + 1) }, Modifier.size(32.dp)) { Icon(Icons.Default.Add, null) }
        }
    }
}

@Composable
fun RoomCard(
    room: Room, 
    onGenerateQR: () -> Unit, 
    onDelete: () -> Unit, 
    onClick: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().zoomClick(onClick = onClick),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val img = if (room.imageUrl.isNotEmpty()) room.imageUrl 
                      else "https://img.freepik.com/free-vector/interior-hotel-room-with-bed-window-sketch_107791-3048.jpg"
            
            AsyncImage(
                model = img, 
                contentDescription = null, 
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), 
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val displayId = if (room.roomNumber.startsWith("R-", ignoreCase = true)) room.roomNumber else "R-${room.roomNumber}"
                Text(text = displayId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "${room.roomType} • ${room.bedType}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = room.isAvailable, 
                    onCheckedChange = { onToggleAvailability() }, 
                    modifier = Modifier.scale(0.7f), 
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2E7D32), uncheckedThumbColor = Color.Red)
                )
                IconButton(onGenerateQR) { Icon(Icons.Default.QrCode, null, tint = MaterialTheme.colorScheme.primary) }
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
        // Use the Web Portal link so guest's phone camera opens the website
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
