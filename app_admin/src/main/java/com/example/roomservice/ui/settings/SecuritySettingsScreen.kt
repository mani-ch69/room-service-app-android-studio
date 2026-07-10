package com.example.roomservice.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomservice.ui.waiter.AdminMenuViewModel
import com.example.roomservice.util.GoogleSheetsHelper

@Composable
fun SecuritySettingsScreen(
    onBackClick: () -> Unit,
    onAppLockClick: () -> Unit,
    adminViewModel: AdminMenuViewModel = viewModel()
) {
    val context = LocalContext.current
    var sheetUrls by remember { mutableStateOf(GoogleSheetsHelper.getSheetUrls(context)) }
    var newSheetUrl by rememberSaveable { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(Pair(0, 0)) }
    val bookings by adminViewModel.bookings.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsCardItem(
                title = "App Lock",
                subtitle = "Manage Mobile PIN and Biometric lock",
                icon = Icons.Default.Lock,
                onClick = onAppLockClick
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(12.dp))
                        Text("Google Sheets Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Manage your Google Sheet links to sync reservations automatically. Each link will start syncing as soon as it's added.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = newSheetUrl,
                        onValueChange = { newSheetUrl = it },
                        label = { Text("Google Sheet Web App URL") },
                        placeholder = { Text("https://script.google.com/macros/s/...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        enabled = !isSyncing
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            val urlToAdd = newSheetUrl.trim()
                            if (urlToAdd.isNotBlank()) {
                                GoogleSheetsHelper.addSheetUrl(context, urlToAdd)
                                sheetUrls = GoogleSheetsHelper.getSheetUrls(context)
                                newSheetUrl = "" // Clear the box immediately
                                
                                isSyncing = true
                                GoogleSheetsHelper.syncAllBookings(
                                    context = context,
                                    bookings = bookings,
                                    specificUrl = urlToAdd,
                                    onProgress = { current, total -> syncProgress = Pair(current, total) },
                                    onComplete = { success ->
                                        isSyncing = false 
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            if (success) {
                                                android.widget.Toast.makeText(context, "Added and synced successfully", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Sync completed with some errors.", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isSyncing && newSheetUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Syncing (${syncProgress.first}/${syncProgress.second})")
                        } else {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add & Sync", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (sheetUrls.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Active Sync Links", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        
                        sheetUrls.forEach { url ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Link, null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = url,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                        color = Color(0xFF334155)
                                    )
                                    IconButton(
                                        onClick = {
                                            GoogleSheetsHelper.removeSheetUrl(context, url)
                                            sheetUrls = GoogleSheetsHelper.getSheetUrls(context)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Remove", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tip: Use Google Sheets 'Data -> Create a filter' to search by Name, Phone, or Booking ID.",
                        fontSize = 10.sp,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
