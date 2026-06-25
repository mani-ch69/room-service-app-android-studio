package com.example.roomservice.ui.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomservice.data.model.TableStatus
import com.example.roomservice.data.model.RestaurantTable

@Composable
fun RestaurantDashboardScreen(
    viewModel: RestaurantViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Restaurant Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color(0xFFFFEBE7)
                        ) {
                            Icon(
                                Icons.Default.Restaurant, 
                                null, 
                                tint = Color(0xFFE64A19),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Restaurant Live",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                    Badge(containerColor = Color(0xFFE64A19)) {
                        Text("LIVE MAP", color = Color.White, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Available", "${uiState.freeTablesCount}", Color(0xFF2E7D32), Modifier.weight(1f))
                    StatCard("Occupied", "${uiState.busyTablesCount}", Color(0xFFD32F2F), Modifier.weight(1f))
                    StatCard("Pending", "${uiState.pendingOrdersCount}", Color(0xFFFBC02D), Modifier.weight(1f))
                }
            }
        }

        // Tabs: Tables vs Active Orders
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFFE64A19),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFFE64A19)
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("TABLES", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("KITCHEN & ORDERS", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> {
                    TableGrid(uiState.tables) { viewModel.onTableClick(it) }
                }
                1 -> {
                    // Orders list would go here
                    EmptyState(Icons.Default.PendingActions, "No active kitchen orders")
                }
            }
        }
    }
}

@Composable
fun TableGrid(tables: List<RestaurantTable>, onTableClick: (RestaurantTable) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(tables) { table ->
            TableItem(table) { onTableClick(table) }
        }
    }
}

@Composable
fun TableItem(table: RestaurantTable, onClick: () -> Unit) {
    val color = when (table.status) {
        TableStatus.FREE -> Color(0xFF2E7D32)
        TableStatus.OCCUPIED -> Color(0xFFD32F2F)
        TableStatus.BILLING -> Color(0xFFFBC02D)
        TableStatus.CLEANING -> Color(0xFF0288D1)
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when(table.status) {
                    TableStatus.FREE -> Icons.Default.TableBar
                    TableStatus.OCCUPIED -> Icons.Default.Person
                    TableStatus.BILLING -> Icons.Default.ReceiptLong
                    TableStatus.CLEANING -> Icons.Default.CleaningServices
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = table.tableNumber, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
            Text(
                text = table.status.name, 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Bold, 
                color = color,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
            Text(text = label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, color = Color.Gray, fontSize = 14.sp)
    }
}
