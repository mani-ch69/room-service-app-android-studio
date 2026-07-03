package com.example.roomservice.ui.waiter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roomservice.data.model.*
import com.example.roomservice.ui.common.ReservationFormContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookingDialog(
    rooms: List<Room>,
    initialBooking: Booking? = null,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val viewModel: AdminMenuViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val allBookings by viewModel.bookings.collectAsState()

    var currentBookingData by remember { mutableStateOf(initialBooking ?: Booking()) }
    var isSaving by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (initialBooking == null) "New Manual Reservation" else "Edit Reservation", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    actions = {
                        if (initialBooking != null && onDeleteClick != null) {
                            IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                delay(800)
                                // Final validation and number generation if new
                                val finalBooking = currentBookingData.copy(
                                    id = initialBooking?.id ?: UUID.randomUUID().toString(),
                                    bookingNumber = if (initialBooking == null) (1000000000L..9999999999L).random().toString() else initialBooking.bookingNumber
                                )
                                onConfirm(finalBooking)
                                isSaving = false
                                showSaveDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        enabled = !isSaving && currentBookingData.guestName.isNotBlank() && currentBookingData.guestPhone.isNotBlank() && currentBookingData.roomNumber.isNotBlank()
                    ) {
                        if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Text("Save Reservation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F9FA)), contentPadding = PaddingValues(16.dp)) {
                item {
                    ReservationFormContent(
                        rooms = rooms,
                        allBookings = allBookings,
                        initialBooking = initialBooking,
                        onDataChanged = { updatedData ->
                            currentBookingData = updatedData
                        }
                    )
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false; onDismiss() },
                title = { Text("Success") },
                text = { Text("Reservation has been saved successfully.") },
                confirmButton = { Button(onClick = { showSaveDialog = false; onDismiss() }) { Text("OK") } }
            )
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
fun SimpleCounter(value: Int, maxValue: Int = Int.MAX_VALUE, onValueChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if(value > 0) onValueChange(value - 1) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Remove, null) }
        Text(text = "$value", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        IconButton(onClick = { if(value < maxValue) onValueChange(value + 1) }, modifier = Modifier.weight(1f), enabled = value < maxValue) { Icon(Icons.Default.Add, null) }
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
