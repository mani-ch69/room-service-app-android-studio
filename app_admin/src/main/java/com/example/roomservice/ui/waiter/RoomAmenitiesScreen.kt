package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.RoomRepository

@Composable
fun RoomAmenitiesScreen() {
    val rooms by RoomRepository.rooms.collectAsState()
    val roomTypes = remember(rooms) { rooms.map { it.roomType }.distinct() }

    val topAmenities = remember {
        listOf(
            "Air conditioning", "Balcony", "View", "Flat-screen TV",
            "Terrace", "Electric kettle", "Toilet paper", "Towels", "Linens"
        )
    }

    // State to store selection for each amenity: "All", "Some", "None"
    // and which room types are selected if "Some" is chosen
    val selections = remember { mutableStateMapOf<String, AmenityState>() }

    // Initialize states
    LaunchedEffect(Unit) {
        topAmenities.forEach { 
            if (!selections.containsKey(it)) {
                selections[it] = AmenityState(selection = "None")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Top Amenities",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "We know these amenities encourage guests to book. Let them know what you have by answering yes or no to each question.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }

            items(topAmenities) { amenity ->
                AmenityItem(
                    label = amenity,
                    state = selections[amenity] ?: AmenityState(selection = "None"),
                    roomTypes = roomTypes,
                    onStateChange = { newState -> selections[amenity] = newState }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 20.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFFE2E8F0)
                )
            }
            
            item {
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = { /* TODO: Save to Firebase */ },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Save Amenities", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class AmenityState(
    val selection: String, // "All", "Some", "None"
    val selectedRoomTypes: Set<String> = emptySet()
)

@Composable
fun AmenityItem(
    label: String,
    state: AmenityState,
    roomTypes: List<String>,
    onStateChange: (AmenityState) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Segmented Control (All / Some / None)
            Row(
                modifier = Modifier
                    .border(1.dp, Color(0xFF1976D2), RoundedCornerShape(4.dp))
                    .height(36.dp)
            ) {
                listOf("All", "Some", "None").forEachIndexed { index, option ->
                    val isSelected = state.selection == option
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(80.dp)
                            .background(
                                if (isSelected) Color(0xFF1976D2) else Color.Transparent,
                                if (index == 0) RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp)
                                else if (index == 2) RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
                                else RoundedCornerShape(0.dp)
                            )
                            .clickable { onStateChange(state.copy(selection = option)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if(option == "All") "All" else if(option == "Some") "Some" else "None",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Color(0xFF1976D2)
                        )
                    }
                    if (index < 2) {
                        VerticalDivider(color = Color(0xFF1976D2), thickness = 1.dp)
                    }
                }
            }
        }

        // Show Room Checkboxes if "Some" is selected
        if (state.selection == "Some") {
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Select where this amenity is available.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                roomTypes.forEach { type ->
                    val isChecked = state.selectedRoomTypes.contains(type)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newList = if (isChecked) state.selectedRoomTypes - type 
                                             else state.selectedRoomTypes + type
                                onStateChange(state.copy(selectedRoomTypes = newList))
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null, // Handled by row click
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1976D2))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = type, fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}
