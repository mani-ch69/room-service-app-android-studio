package com.example.roomservice.ui.guest

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.roomservice.data.MenuRepository
import com.example.roomservice.data.model.MenuItem
import com.example.roomservice.data.model.OrderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestMenuScreen(
    onBackClick: () -> Unit,
    onViewCartClick: (List<OrderItem>) -> Unit
) {
    val categories by MenuRepository.categories.collectAsState()
    val menuItems by MenuRepository.menuItems.collectAsState()
    
    var selectedCategory by remember { mutableStateOf("All") }
    val cartItems = remember { mutableStateMapOf<String, OrderItem>() }
    
    val filteredItems = if (selectedCategory == "All") {
        menuItems
    } else {
        menuItems.filter { it.category == selectedCategory }
    }

    val totalItemsInCart = cartItems.values.sumOf { it.quantity }
    val totalAmount = cartItems.values.sumOf { it.price * it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Menu", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (totalItemsInCart > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { onViewCartClick(cartItems.values.toList()) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2E7D32),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("$totalItemsInCart Items added", color = Color.White, fontSize = 12.sp)
                            Text("Total: ₹$totalAmount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("VIEW CART", color = Color.White, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ShoppingCart, null, tint = Color.White)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Category Filter
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChip("All", selectedCategory == "All") { selectedCategory = "All" }
                }
                items(categories) { category ->
                    CategoryChip(category.name, selectedCategory == category.name) { selectedCategory = category.name }
                }
            }

            // Menu Items List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredItems) { item ->
                    if (item.isAvailable) {
                        GuestMenuItemCard(
                            item = item,
                            quantity = cartItems[item.id]?.quantity ?: 0,
                            onQuantityChange = { newQty ->
                                if (newQty > 0) {
                                    cartItems[item.id] = OrderItem(item.id, item.name, item.price, newQty)
                                } else {
                                    cartItems.remove(item.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF1976D2) else Color(0xFFF1F3F4),
        modifier = Modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                name, 
                color = if (isSelected) Color.White else Color.Black, 
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun GuestMenuItemCard(
    item: MenuItem,
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item Image
            AsyncImage(
                model = item.imageUrl.ifEmpty { "https://img.freepik.com/free-photo/delicious-indian-food-tray_23-2148723505.jpg" },
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(16.dp))

            // Item Details
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(item.description, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Text("₹${item.price}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1976D2))
            }

            // Quantity Selector
            if (quantity == 0) {
                Button(
                    onClick = { onQuantityChange(1) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("ADD", color = Color(0xFF1976D2), fontWeight = FontWeight.Black)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF1976D2), RoundedCornerShape(8.dp))
                        .height(32.dp)
                ) {
                    IconButton(onClick = { onQuantityChange(quantity - 1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text("$quantity", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    IconButton(onClick = { onQuantityChange(quantity + 1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
