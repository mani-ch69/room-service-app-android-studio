package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReservationPoliciesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Reservation policies",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "All your cancellation and prepayment policies are here. You can view, manage, and edit everything in one place.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Text(
                    text = "Cancellation & Prepayment Policies",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Flexible - 7 days (General)
            item {
                PolicyDetailCard(
                    title = "Flexible - 7 days (General)",
                    description = listOf(
                        "The guest can cancel free of charge until 7 days before arrival. The guest will be charged the cost of the first night if they cancel within 7 days of arrival.",
                        "No prepayment is needed."
                    ),
                    reportData = mapOf(
                        "Report from" to "Apr 4, 2024 to Jul 3, 2024",
                        "Total Room Nights" to "12",
                        "Total Revenue" to "INR 7127.78"
                    ),
                    primaryAction = "Edit",
                    primaryActionColor = Color(0xFF1976D2)
                )
            }

            // Non-refundable (Non Refundable)
            item {
                PolicyDetailCard(
                    title = "Non-refundable (Non Refundable)",
                    description = listOf(
                        "The guest will be charged the total price of the reservation if they cancel anytime. If the guest doesn't show up, they'll be charged the total price of the reservation.",
                        "The guest will be charged a prepayment of the total price of the reservation at any time."
                    ),
                    primaryAction = "Delete",
                    primaryActionColor = Color(0xFFC62828)
                )
            }

            // Create New Policy Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("You can create 4 more policies.", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Create new cancellation policy", fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PolicyDetailCard(
    title: String,
    description: List<String>,
    reportData: Map<String, String>? = null,
    primaryAction: String,
    primaryActionColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Main Content Area
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                description.forEach { item ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("•", modifier = Modifier.padding(end = 8.dp), color = Color.Gray)
                        Text(text = item, fontSize = 11.sp, color = Color.DarkGray, lineHeight = 16.sp)
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryActionColor),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text(primaryAction, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2)),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Apply to other properties", fontSize = 11.sp, color = Color(0xFF1976D2))
                    }
                }
            }

            // Report Area (if any)
            if (reportData != null) {
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFF8FAFC))
                        .padding(12.dp)
                ) {
                    reportData.forEach { (key, value) ->
                        Text(key, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(value, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0), modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        }
    }
}
