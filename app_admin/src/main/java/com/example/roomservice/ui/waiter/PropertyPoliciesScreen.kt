package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PropertyPoliciesScreen() {
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
                    text = "Property policies",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "All your property-related policy info is here. You can view, manage, and edit everything in one place.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Text(
                    text = "Children & Extra Beds",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Flexible Child Rates Banner
            item {
                Surface(
                    color = Color(0xFFFFF7E6),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD591)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFFAAD14), modifier = Modifier.size(20.dp))
                        Text(
                            text = "You can create even more tailored child rates with the new flexible child rates. Learn more here.",
                            fontSize = 12.sp,
                            color = Color(0xFF874D00)
                        )
                    }
                }
            }

            // Children Policies Card
            item {
                PolicySectionCard(
                    title = "Children policies",
                    subSections = listOf(
                        PolicySubSection(
                            title = "Child policies",
                            items = listOf("Children 17 and older are allowed.")
                        ),
                        PolicySubSection(
                            title = "Children rates",
                            items = listOf("Children 17 years old can stay for 10.00% of the adult price per child, per night.")
                        )
                    )
                )
            }

            // Extra Bed & Crib Options Card
            item {
                PolicySectionCard(
                    title = "Extra bed & crib options",
                    subSections = listOf(
                        PolicySubSection(
                            title = "Cribs",
                            items = listOf("You haven't added any cribs.")
                        ),
                        PolicySubSection(
                            title = "Extra beds",
                            items = listOf("You haven't added any extra beds.")
                        )
                    )
                )
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

data class PolicySubSection(
    val title: String,
    val items: List<String>
)

@Composable
fun PolicySectionCard(title: String, subSections: List<PolicySubSection>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(12.dp)
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Column(modifier = Modifier.padding(16.dp)) {
                subSections.forEachIndexed { index, sub ->
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(text = sub.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(Modifier.height(8.dp))
                        sub.items.forEach { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", modifier = Modifier.padding(end = 8.dp), color = Color.Gray)
                                Text(text = item, fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = { /* Handle Edit */ },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
                ) {
                    Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
