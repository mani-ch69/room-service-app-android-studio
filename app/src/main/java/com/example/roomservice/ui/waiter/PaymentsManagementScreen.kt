package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomservice.data.model.OrderStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaymentsManagementScreen(
    adminViewModel: AdminMenuViewModel = viewModel()
) {
    val allOrders by adminViewModel.allOrders.collectAsState()
    val bookings by adminViewModel.bookings.collectAsState()

    val totalOrderRevenue = remember(allOrders) {
        allOrders.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.totalAmount }
    }

    val totalBookingRevenue = remember(bookings) {
        bookings.sumOf { it.advancePaid } // Just counting advances for now as manual cash
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .padding(16.dp)
    ) {
        // SUMMARY CARDS
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RevenueCard(
                title = "Order Revenue",
                amount = totalOrderRevenue,
                color = Color(0xFF1976D2),
                modifier = Modifier.weight(1f)
            )
            RevenueCard(
                title = "Booking Advance",
                amount = totalBookingRevenue,
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Recent Transactions (Cash)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )

        Spacer(Modifier.height(12.dp))

        // COMBINED LIST
        val transactions = remember(allOrders, bookings) {
            val list = mutableListOf<TransactionItem>()
            
            allOrders.filter { it.status == OrderStatus.DELIVERED }.forEach {
                list.add(TransactionItem(
                    id = it.id,
                    title = "Order - Room ${it.roomNumber}",
                    amount = it.totalAmount,
                    timestamp = it.timestamp,
                    type = "Order"
                ))
            }

            bookings.forEach {
                if (it.advancePaid > 0) {
                    list.add(TransactionItem(
                        id = it.id,
                        title = "Booking Advance - Room ${it.roomNumber}",
                        amount = it.advancePaid,
                        timestamp = it.timestamp,
                        type = "Booking"
                    ))
                }
            }

            list.sortedByDescending { it.timestamp }
        }

        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("No transactions found", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { tx ->
                    TransactionRow(tx)
                }
            }
        }
    }
}

data class TransactionItem(
    val id: String,
    val title: String,
    val amount: Double,
    val timestamp: Long,
    val type: String
)

@Composable
fun RevenueCard(title: String, amount: Double, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            Text("₹$amount", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun TransactionRow(tx: TransactionItem) {
    val time = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(time, fontSize = 11.sp, color = Color.Gray)
            }
            Text(
                text = "+₹${tx.amount}",
                fontWeight = FontWeight.ExtraBold,
                color = if (tx.type == "Order") Color(0xFF1976D2) else Color(0xFF2E7D32),
                fontSize = 16.sp
            )
        }
    }
}
