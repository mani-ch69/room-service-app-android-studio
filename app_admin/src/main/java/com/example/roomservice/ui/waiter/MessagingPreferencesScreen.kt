package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roomservice.data.WhatsAppTemplateRepository
import com.example.roomservice.data.model.WhatsAppTemplate
import com.example.roomservice.util.TemplateEngine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MessagingPreferencesScreen() {
    val context = LocalContext.current
    var templates by remember { mutableStateOf(WhatsAppTemplateRepository.getTemplates(context)) }
    var editingTemplate by remember { mutableStateOf<WhatsAppTemplate?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // HEADER
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Messaging Preferences",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        "Manage your WhatsApp message templates",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                TextButton(onClick = { showResetConfirm = true }) {
                    Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset All", fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(templates, key = { it.id }) { template ->
                TemplateItemCard(
                    template = template,
                    onEdit = { editingTemplate = template }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (editingTemplate != null) {
        EditTemplateDialog(
            template = editingTemplate!!,
            onDismiss = { editingTemplate = null },
            onSave = { updated ->
                WhatsAppTemplateRepository.updateTemplate(context, updated)
                templates = WhatsAppTemplateRepository.getTemplates(context)
                editingTemplate = null
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Templates?") },
            text = { Text("This will restore all WhatsApp templates to their original default content. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        WhatsAppTemplateRepository.resetToDefault(context)
                        templates = WhatsAppTemplateRepository.getTemplates(context)
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("RESET")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun TemplateItemCard(template: WhatsAppTemplate, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = template.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Text(
                    text = template.content,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTemplateDialog(
    template: WhatsAppTemplate,
    onDismiss: () -> Unit,
    onSave: (WhatsAppTemplate) -> Unit
) {
    var content by remember { mutableStateOf(template.content) }
    var showPreview by remember { mutableStateOf(false) }

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
                    title = { Text("Edit Template", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        TextButton(onClick = { onSave(template.copy(content = content)) }) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = template.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                            placeholder = { Text("Type your message here...") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                        )
                    }

                    item {
                        Text(
                            "Variables", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp, 
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TemplateEngine.placeholders.forEach { variable ->
                                SuggestionChip(
                                    onClick = { content += variable },
                                    label = { Text(variable, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { showPreview = !showPreview },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (showPreview) "Hide Preview" else "Show Sample Preview")
                        }
                    }

                    if (showPreview) {
                        item {
                            Text("Preview (Sample Data)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            
                            // Sample Preview Logic
                            val sampleBooking = com.example.roomservice.data.model.Booking(
                                guestName = "John Doe",
                                guestPhone = "+91 9876543210",
                                bookingNumber = "BK-12345",
                                roomType = "Deluxe Room",
                                roomNumber = "101",
                                checkInDate = System.currentTimeMillis(),
                                checkOutDate = System.currentTimeMillis() + 86400000L,
                                totalAmount = 5000.0,
                                advancePaid = 1500.0
                            )
                            
                            val resolved = TemplateEngine.resolve(content, sampleBooking, null)
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFE7F3FF),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBDEFB))
                            ) {
                                Text(
                                    text = resolved,
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp,
                                    color = Color(0xFF0D47A1)
                                )
                            }
                        }
                    }
                    
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}
