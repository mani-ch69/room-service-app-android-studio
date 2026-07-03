package com.example.roomservice.ui.waiter

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ValueAddsScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("About", "Manage value adds")

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = Color(0xFF1976D2),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFF1976D2)
                )
            },
            divider = { HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontSize = 13.sp, fontWeight = if(selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium) }
                )
            }
        }

        if (selectedTabIndex == 0) {
            AboutValueAddsTab()
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Manage Value Adds Coming Soon", color = Color.Gray)
            }
        }
    }
}

@Composable
fun AboutValueAddsTab() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Text(
            text = "About",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(Modifier.height(40.dp))

        // Illustration Area
        Box(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            // Using a professional illustration placeholder
            AsyncImage(
                model = "https://i.ibb.co/VvzK2mB/value-adds-illustration.png",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Introducing value adds",
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Boost revenue and attract more guests with value adds",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 36.sp,
            color = Color(0xFF1E293B)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { },
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
            modifier = Modifier.height(44.dp).padding(horizontal = 4.dp)
        ) {
            Text("Create value adds", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        
        Spacer(Modifier.height(40.dp))
    }
}
