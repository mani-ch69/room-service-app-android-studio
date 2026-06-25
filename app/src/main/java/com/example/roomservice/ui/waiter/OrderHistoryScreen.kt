package com.example.roomservice.ui.waiter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.OrderRepository
import com.example.roomservice.data.model.Order
import com.example.roomservice.data.model.OrderStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrderHistoryContent() {
    val orders by OrderRepository.orders.collectAsState()
    
    val historyOrders = orders.filter { 
        it.status == OrderStatus.DELIVERED || it.status == OrderStatus.CANCELLED 
    }.sortedByDescending { it.timestamp }

    if (historyOrders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No order history found", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(historyOrders) { order ->
                OrderHistoryItem(order)
            }
        }
    }
}

@Composable
fun OrderHistoryItem(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Room ${order.roomNumber}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                StatusBadge(order.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            order.items.forEach { item ->
                Text(text = "• ${item.quantity}x ${item.name}", fontSize = 14.sp, color = Color.DarkGray)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.timestamp)),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Total: ₹${order.totalAmount}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
