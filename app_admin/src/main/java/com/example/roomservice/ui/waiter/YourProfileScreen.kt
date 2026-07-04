package com.example.roomservice.ui.waiter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun YourProfileScreen() {
    var profileType by remember { mutableStateOf("Host profile") }
    var hostName by remember { mutableStateOf("manish chaurasiya") }
    var aboutProperty by remember { mutableStateOf("pawan-watika/vrindavan... especially useful for bloggers, digital nomads...") }
    var aboutHost by remember { mutableStateOf("") }
    var aboutNeighborhood by remember { mutableStateOf("Nearby Attractions: \n- Varanasi Railway Junction - 2.8 km\n- Kashi Vishwanath Temple - 3.4 km") }
    
    var openMonth by remember { mutableStateOf("February") }
    var openYear by remember { mutableStateOf("2024") }
    var builtYear by remember { mutableStateOf("1982") }
    var liveOnSite by remember { mutableStateOf("On-site") }

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
                Text("Your Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(
                    "This info can be shown to potential guests on our website and is an opportunity to help your property stand out.",
                    fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)
                )
            }

            // PROFILE TYPE
            item {
                ProfileCard(title = "Profile type") {
                    Column {
                        Text("Pick \"Host profile\" if you personally manage your property or \"Company profile\" if you're a business.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = profileType == "Host profile", onClick = { profileType = "Host profile" })
                            Text("Host profile", fontSize = 13.sp)
                            Spacer(Modifier.width(16.dp))
                            RadioButton(selected = profileType == "Company profile", onClick = { profileType = "Company profile" })
                            Text("Company profile", fontSize = 13.sp)
                        }
                    }
                }
            }

            // BASIC INFO
            item {
                ProfileCard(title = "Basic info") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("Host name", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = hostName, onValueChange = { hostName = it },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                            Text("${82 - hostName.length} characters left", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                        }
                        
                        Column {
                            Text("Profile image", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Add a photo of yourself or the team that will welcome guests. A photo showing your face is the best way to add a personal touch to your profile's public page. Just don't include any personal contact details.", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(
                                        model = "https://i.pravatar.cc/300?img=12", contentDescription = null,
                                        modifier = Modifier.size(100.dp).clip(CircleShape).border(1.dp, Color.LightGray, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { }, shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("Upload logo or photo", fontSize = 12.sp) }
                                        Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)), shape = RoundedCornerShape(4.dp)) { Text("Delete", fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // DETAILED INFO
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Detailed info", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    
                    // COVID NOTICE
                    Surface(
                        color = Color(0xFFFFF7ED), border = BorderStroke(1.dp, Color(0xFFFDBA74)), shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Coronavirus (COVID-19) guidelines", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                }
                                Text("To be in line with General Data Protection, we don't allow any mention of coronavirus (COVID-19) in the property description or about host sections. If your profile is in multiple languages, this applies to all translations as well.", fontSize = 11.sp, color = Color(0xFF9A3412))
                            }
                        }
                    }

                    // TABS
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("English", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                        Text("Add a language", color = Color(0xFF1976D2), fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    }
                    HorizontalDivider(thickness = 2.dp, color = Color(0xFF1976D2), modifier = Modifier.width(80.dp))

                    ProfileCard(title = "About the Property") {
                        Column {
                            Text("What makes your place unique and how can you help guests feel more welcome? Think about decor, amenities, and special features. Don't add House Rules here; they gain under the Policies section.", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = aboutProperty, onValueChange = { aboutProperty = it }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(4.dp))
                        }
                    }

                    ProfileCard(title = "About the Host") {
                        Column {
                            Text("Help guests feel at ease and excited about their trip with a short welcome message. What do you (or your team) enjoy about hosting? Share personal interests or hobbies.", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = aboutHost, onValueChange = { aboutHost = it }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(4.dp))
                        }
                    }

                    ProfileCard(title = "About the Neighborhood") {
                        Column {
                            Text("What do guests like most about the neighborhood? Include tips about the area, local attractions, and points of interest such as museums, restaurants, or famous landmarks.", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = aboutNeighborhood, onValueChange = { aboutNeighborhood = it }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(4.dp))
                        }
                    }
                }
            }

            // ABOUT YOUR PROPERTY
            item {
                ProfileCard(title = "About Your Property") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Additional info about your property (optional)", fontSize = 12.sp, color = Color.Gray)
                        
                        Column {
                            Text("When did the property open?", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = openMonth, onValueChange = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(4.dp), readOnly = true)
                                OutlinedTextField(value = openYear, onValueChange = { openYear = it }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(4.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                        }

                        Column {
                            Text("When was the property built?", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = builtYear, onValueChange = { builtYear = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }

                        Column {
                            Text("Do you live on site or off site?", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = liveOnSite, onValueChange = { }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp), readOnly = true)
                        }

                        Column {
                            Text("What was renovated?", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Column {
                                listOf("Entire property", "Rooms in property", "Common areas", "Facilities", "Other").forEach {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = it == "Rooms in property", onCheckedChange = {})
                                        Text(it, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "By saving this content, I agree to let admin69 use my full profile on its website, mobile website, apps and in all other means of communication.",
                    fontSize = 10.sp, color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { }, modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) { Text("Save", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ProfileCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
