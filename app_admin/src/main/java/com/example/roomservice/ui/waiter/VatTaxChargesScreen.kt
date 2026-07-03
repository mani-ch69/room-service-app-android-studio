package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VatTaxChargesScreen() {
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
                    text = "VAT/Tax/Charges",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "This overview shows your local taxes and fees, such as VAT, city tax, and service charges. If any of the fees need to be adjusted, contact us for support.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Column {
                        // Header Table Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                                .padding(12.dp)
                        ) {
                            Spacer(modifier = Modifier.weight(0.4f))
                            Text("Now", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("Most popular in Varanasi", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        // Content Row: GST
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "Goods & services tax", 
                                modifier = Modifier.weight(0.4f), 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color.DarkGray
                            )
                            
                            // "Now" Column
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                                Text("Goods & services tax of ₹ 0 progressive isn't included", fontSize = 11.sp)
                                Text("Conditions apply", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                
                                Spacer(Modifier.height(12.dp))
                                
                                // Mini Table for Progressive rates
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                        Text("Progressive rates", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Text("Percentage", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Text("₹ 0 - ₹ 7500", modifier = Modifier.weight(1f), fontSize = 10.sp)
                                        Text("5%", modifier = Modifier.weight(1f), fontSize = 10.sp)
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Text("₹ 7501 and above", modifier = Modifier.weight(1f), fontSize = 10.sp)
                                        Text("18%", modifier = Modifier.weight(1f), fontSize = 10.sp)
                                    }
                                }
                            }

                            // "Popular" Column
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                                Text("Goods & services tax of ₹ 1 progressive isn't included", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "* The most common value (VAT, city tax, service charge) out of a total of 2176 open properties in Varanasi.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
