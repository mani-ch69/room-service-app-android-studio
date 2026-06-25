package com.example.roomservice.ui.waiter

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.model.Staff

@Composable
fun StaffManagementScreen(
    staffList: List<Staff>,
    onAddStaffClick: (String?) -> Unit,
    onEditStaff: (Staff) -> Unit,
    onDeleteStaff: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val securityManager = remember { com.example.roomservice.util.SecurityManager(context) }
    
    val defaultRoles = listOf("Restaurant", "Waiter", "Housekeeping", "Shop", "Delivery Agent")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val currentRole = defaultRoles[selectedTabIndex]
    val filteredStaff = staffList.filter { it.role == currentRole }
    
    var staffToDelete by remember { mutableStateOf<Staff?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9))) {
        // ... (rest of the code)
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = Color.White,
            contentColor = Color(0xFF007BFF),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = 3.dp,
                    color = Color(0xFF007BFF)
                )
            },
            divider = { HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0)) }
        ) {
            defaultRoles.forEachIndexed { index, role ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = role,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == index) Color(0xFF007BFF) else Color(0xFF64748B)
                        )
                    }
                )
            }
        }

        // --- ROLE SPECIFIC CONTENT ---
        Box(modifier = Modifier.weight(1f).background(Color.White)) {
            if (filteredStaff.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9)
                        ) {
                            Icon(
                                Icons.Default.Group, 
                                null, 
                                modifier = Modifier.padding(20.dp),
                                tint = Color.LightGray
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("No $currentRole Staff", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("Tap the button below to add", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            } else {
                StaffListContent(
                    staffList = filteredStaff, 
                    onEdit = onEditStaff,
                    onDeleteRequest = { staffToDelete = it }
                )
            }
        }

        // --- ADD BUTTON ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = { onAddStaffClick(currentRole) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(12.dp))
                Text("ADD ${currentRole.uppercase()} STAFF", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (staffToDelete != null) {
        StaffDeleteConfirmationDialog(
            staff = staffToDelete!!,
            savedMpin = securityManager.getPin(),
            onDismiss = { staffToDelete = null },
            onConfirm = {
                onDeleteStaff(staffToDelete!!.id)
                staffToDelete = null
            }
        )
    }
}

@Composable
fun StaffDeleteConfirmationDialog(
    staff: Staff,
    savedMpin: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var mpinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
        title = { Text("Remove Staff Member?") },
        text = {
            Column {
                Text("Are you sure you want to remove ${staff.name} from the team?")
                Text("This action cannot be undone.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                Spacer(Modifier.height(16.dp))
                Text("Confirm with Admin MPIN:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = mpinInput,
                    onValueChange = { 
                        if (it.length <= 6) {
                            mpinInput = it
                            isError = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter 6-digit MPIN") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = isError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                if (isError) {
                    Text("Incorrect MPIN. Please try again.", color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mpinInput == savedMpin) {
                        onConfirm()
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = mpinInput.length == 6
            ) {
                Text("REMOVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun StaffListContent(
    staffList: List<Staff>,
    onEdit: (Staff) -> Unit,
    onDeleteRequest: (Staff) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        items(staffList.asReversed()) { staff ->
            ManageStaffListItem(
                staff = staff, 
                onClick = { onEdit(staff) },
                onDelete = { onDeleteRequest(staff) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))
        }
    }
}

@Composable
fun ManageStaffListItem(
    staff: Staff,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showStaffQR by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF388E3C)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = staff.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = staff.name, 
                fontWeight = FontWeight.Bold, 
                fontSize = 16.sp,
                color = Color.Black
            )
            Text(
                text = "Staff ID: RS-${staff.code} • ${staff.role}", 
                color = Color.Gray, 
                fontSize = 13.sp
            )
        }

        IconButton(onClick = { showStaffQR = true }) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = "Staff QR",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Remove Staff",
                tint = Color.Red.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showStaffQR) {
        StaffQRDialog(staff = staff, onDismiss = { showStaffQR = false })
    }
}

@Composable
fun StaffQRDialog(staff: Staff, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    
    // Including Staff UUID (id) for correct task filtering on dashboard
    val qrData = "roomservice://staff_login?" +
            "id=${staff.id}&" +
            "hotelId=${staff.hotelId}&" +
            "code=${staff.code}&" +
            "name=${android.net.Uri.encode(staff.name)}&" +
            "phone=${staff.phone}&" +
            "role=${staff.role}"

    val qrBitmap = remember(staff.code) { com.example.roomservice.util.QRCodeGenerator.generate(qrData) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Staff Login Setup", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(staff.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("Staff ID: RS-${staff.code}", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), fontSize = 14.sp)
                Text(staff.role, color = Color.Gray, fontSize = 14.sp)
                
                Spacer(Modifier.height(24.dp))
                
                Surface(
                    modifier = Modifier.size(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        qrBitmap?.let { bitmap ->
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(180.dp)
                            )
                        } ?: Text("QR Error", color = Color.Red)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { 
                        clipboardManager.setText(AnnotatedString(qrData))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1976D2)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1976D2))
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("COPY LOGIN LINK", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "Scan QR or send the link to staff for login without a camera.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) {
                Text("CLOSE")
            }
        }
    )
}

@Composable
fun StaffEmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        
        Box(
            modifier = Modifier
                .size(300.dp, 180.dp)
                .clip(RoundedCornerShape(90.dp))
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(80.dp), tint = Color(0xFF673AB7))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, null, Modifier.size(24.dp), tint = Color.Gray)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(24.dp), tint = Color.Blue)
                    Icon(Icons.Default.AssignmentInd, null, Modifier.size(40.dp), tint = Color.LightGray)
                }
            }
        }
        
        Spacer(Modifier.height(48.dp))
        
        Text(
            "Get your team onboard",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            "There are no users linked to your account yet",
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
        ) {
            Text("+ ADD NEW STAFF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun UserProfileScreen(
    staffName: String,
    onNameChange: (String) -> Unit,
    staffPhone: String,
    onPhoneChange: (String) -> Unit,
    selectedRole: String,
    onRoleSelect: (String) -> Unit,
    onAddUser: () -> Unit
) {
    val roles = listOf("Restaurant", "Waiter", "Housekeeping", "Shop", "Delivery Agent", "Others")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("User Details", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Spacer(Modifier.height(16.dp))
                        
                        Text("Name", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = staffName,
                            onValueChange = onNameChange,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            placeholder = { Text("Enter Name") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFFDDDDDD)
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        Text("Phone Number", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = staffPhone,
                            onValueChange = onPhoneChange,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            placeholder = { Text("Enter Mobile Number") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFFDDDDDD)
                            )
                        )
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("User Role", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Spacer(Modifier.height(16.dp))
                        
                        StaffFlowRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            roles.forEach { role ->
                                RoleChip(role, selectedRole == role) { onRoleSelect(role) }
                            }
                        }
                    }
                }
            }
        }
        
        Button(
            onClick = onAddUser,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
            enabled = staffName.isNotBlank() && staffPhone.length >= 10
        ) {
            Text("SAVE STAFF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffFlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: Dp = 8.dp,
    crossAxisSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing),
        content = { content() }
    )
}

@Composable
fun RoleChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(21.dp),
        color = if (isSelected) Color(0xFF007BFF) else Color.White,
        border = if (isSelected) null else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (isSelected) Color.White else Color.Black, fontSize = 14.sp)
            if (isSelected) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
            }
        }
    }
}

@Composable
fun StaffSuccessScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        
        Box(
            modifier = Modifier
                .size(300.dp, 180.dp)
                .clip(RoundedCornerShape(90.dp))
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.EmojiPeople, null, modifier = Modifier.size(80.dp), tint = Color(0xFF673AB7))
                Spacer(Modifier.height(8.dp))
                Surface(shape = CircleShape, color = Color.White, shadowElevation = 2.dp) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(40.dp).padding(4.dp), tint = Color(0xFF4CAF50))
                }
            }
        }
        
        Spacer(Modifier.height(60.dp))
        
        Text(
            "User added successfully!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "The user can now login to the Room Service Admin and access the hotel details with the roles you have provided.",
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 20.sp
        )
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
        ) {
            Text("DONE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
