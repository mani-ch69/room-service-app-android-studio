package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicRestrictionsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Dynamic restriction rules",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Restrictions can be useful but they can also limit visibility. Dynamic rules enable you to adjust prices and restrictions based on your needs.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Manage your rules Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Manage your rules", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("How multiple rules work together", fontSize = 11.sp, color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline, modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Horizontal scroll for table on small screens
                        Box(modifier = Modifier.fillMaxWidth()) {
                            RulesPreviewTable()
                        }
                    }
                }
            }

            // Unsold nights info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Unsold nights", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("What are unsold nights?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Unsold nights are upcoming nights that have remained vacant. Guests may not be able to see and book your property due to your restrictions.", fontSize = 12.sp, color = Color.Gray)
                        Text("View current unsold nights", fontSize = 11.sp, color = Color(0xFF1976D2), modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            // Create a rule for unsold nights
            item {
                Text("Create a rule for unsold nights", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Set up how your restrictions will be relaxed for unsold nights.", fontSize = 12.sp, color = Color.Gray)
            }

            // Restrictions Card
            item {
                RuleConfigCard(title = "Restrictions") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("What will your restrictions be for unsold nights?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Select how long guests need to stay and how far in advance they need to book.", fontSize = 11.sp, color = Color.Gray)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Minimum length of stay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                RestrictionDropdown("1 night")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Minimum advance (optional)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                RestrictionDropdown("Select")
                            }
                        }
                    }
                }
            }

            // Price Card
            item {
                RuleConfigCard(title = "Price") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Would you like a price change when restrictions are relaxed?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("For example, you can increase your price when guests book with relaxed restrictions.", fontSize = 11.sp, color = Color.Gray)
                        
                        Text("Price change", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        RestrictionDropdown("No price change")
                    }
                }
            }

            // Customize your rule Card
            item {
                RuleConfigCard(title = "Customize your rule") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RuleSummaryLine("Applicable period", "30 days")
                        RuleSummaryLine("Room types and rate plans", "All room types, all rate plans")
                        RuleSummaryLine("End date", "Apply until I turn it off")
                    }
                }
            }

            item {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Continue to review", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun RuleConfigCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun RestrictionDropdown(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
        }
    }
}

@Composable
fun RuleSummaryLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(label, fontSize = 11.sp, color = Color.Gray)
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text("Edit", color = Color(0xFF1976D2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
    }
}

@Composable
fun RulesPreviewTable() {
    // Simple table representation for mobile
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(8.dp)) {
            Text("Rule type", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Price", modifier = Modifier.weight(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Status", modifier = Modifier.weight(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        RuleRow("For gap nights", "Increase by 10%", "ON")
        RuleRow("For unsold nights", "Decrease by 10%", "ON")
    }
}

@Composable
fun RuleRow(type: String, price: String, status: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(type, modifier = Modifier.weight(1f), fontSize = 11.sp)
        Text(price, modifier = Modifier.weight(0.6f), fontSize = 11.sp)
        Text(status, modifier = Modifier.weight(0.4f), fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
}
