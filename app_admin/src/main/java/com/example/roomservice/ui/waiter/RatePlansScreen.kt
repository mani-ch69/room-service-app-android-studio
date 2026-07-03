package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
fun RatePlansScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add a new rate plan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2)),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("Back", fontSize = 12.sp, color = Color(0xFF1976D2))
                    }
                }
            }

            // Category 1: Increase bookings
            item {
                RatePlanCategorySection(
                    title = "Increase bookings and reduce cancellations",
                    subtitle = "These rate plans will give you a solid foundation for your pricing strategy.",
                    plans = listOf(
                        RatePlanItemData("Flexible", "Let guests cancel for free, and they'll boost your bookings and revenue"),
                        RatePlanItemData("Firm", "Strengthen your competitive edge without having to worry about last-minute cancellations with this 14-day cancellation policy"),
                        RatePlanItemData("Non-refundable", "Reduce cancellations by attracting guests who are sure of their dates.")
                    )
                )
            }

            // Category 2: Attract wider range
            item {
                RatePlanCategorySection(
                    title = "Attract a wider range of guests",
                    subtitle = "After establishing your foundation, these rate plans can help you build on it and make your property more appealing to high-value user groups.",
                    plans = listOf(
                        RatePlanItemData("Weekly", "Reach out to guests looking to stay longer than 1 week"),
                        RatePlanItemData("Monthly", "Earn more stable income from guests after day 28+ nights"),
                        RatePlanItemData("Early booker", "Get more bookings earlier by attracting guests who plan ahead")
                    )
                )
            }

            // Category 3: Custom
            item {
                RatePlanCategorySection(
                    title = "Custom plan",
                    subtitle = "Set up a rate plan tailored to your property and business goals.",
                    plans = listOf(
                        RatePlanItemData("Custom", "Customize a rate plan to suit your needs.")
                    )
                )
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

data class RatePlanItemData(
    val title: String,
    val description: String
)

@Composable
fun RatePlanCategorySection(title: String, subtitle: String, plans: List<RatePlanItemData>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
        Text(text = subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
        
        Spacer(Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2E8F0))
        ) {
            Column {
                plans.forEachIndexed { index, plan ->
                    RatePlanRow(plan)
                    if (index < plans.size - 1) {
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
fun RatePlanRow(plan: RatePlanItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = plan.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            Spacer(Modifier.height(4.dp))
            Text(text = plan.description, fontSize = 11.sp, color = Color.Gray, lineHeight = 14.sp)
        }
        
        OutlinedButton(
            onClick = { },
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2)),
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text("Add rate plan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
        }
    }
}
