package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roomservice.data.model.Booking
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit
) {
    var totalAmount by remember { mutableStateOf(booking.totalAmount.toInt().toString()) }
    var discount by remember { mutableStateOf(booking.discount.toInt().toString()) }
    var advancePaid by remember { mutableStateOf(booking.advancePaid.toInt().toString()) }
    
    // Auto-fill logic: Initialize paymentAmount with remaining amount
    val initialRemaining = remember(booking) { 
        (booking.totalAmount - booking.discount - booking.advancePaid).coerceAtLeast(0.0).toInt().toString()
    }
    var paymentAmount by remember { mutableStateOf(initialRemaining) }
    
    var paymentMethod by remember { mutableStateOf(booking.paymentMode) }
    var paymentType by remember { mutableStateOf("Advance") }
    var upiId by remember { mutableStateOf(booking.upiTransactionId) }
    var receiptNumber by remember { mutableStateOf(booking.receiptNumber.ifBlank { "REC-${(1000..9999).random()}" }) }
    var receivedBy by remember { mutableStateOf(booking.receivedBy) }
    
    var paymentDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val sdfDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val currentRemaining = remember(totalAmount, discount, advancePaid, paymentAmount) {
        val total = totalAmount.toDoubleOrNull() ?: 0.0
        val disc = discount.toDoubleOrNull() ?: 0.0
        val currentAdv = advancePaid.toDoubleOrNull() ?: 0.0
        val newPay = paymentAmount.toDoubleOrNull() ?: 0.0
        (total - disc - currentAdv - newPay).coerceAtLeast(0.0)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add / Adjust Payment", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Payments, null, tint = Color(0xFF1976D2))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val newAdvance = (advancePaid.toDoubleOrNull() ?: 0.0) + (paymentAmount.toDoubleOrNull() ?: 0.0)
                                onConfirm(booking.copy(
                                    totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                                    discount = discount.toDoubleOrNull() ?: 0.0,
                                    advancePaid = newAdvance,
                                    paymentMode = paymentMethod,
                                    upiTransactionId = upiId,
                                    receiptNumber = receiptNumber,
                                    receivedBy = receivedBy,
                                    paymentType = paymentType,
                                    paymentDate = paymentDate
                                ))
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Save Payment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION: Booking Info & Financial Summary at top
                item {
                    PaymentFormSection("Booking Details") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                ReadOnlyField("Booking ID", booking.bookingNumber)
                                ReadOnlyField("Guest Name", booking.guestName)
                            }
                            
                            Divider(color = Color(0xFFEEEEEE))
                            
                            // FINANCIAL SUMMARY AT TOP
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SummaryRow("Total Amount", "₹ ${totalAmount.ifBlank { "0" }}")
                                SummaryRow("Paid Amount", "₹ ${advancePaid.ifBlank { "0" }}")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Remaining Amount", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                                    Text("₹ ${currentRemaining.toInt()}", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }

                // SECTION: Payment Details
                item {
                    PaymentFormSection("Payment Information") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = paymentAmount,
                                onValueChange = { if (it.all { char -> char.isDigit() }) paymentAmount = it },
                                label = { Text("Payment Amount *") },
                                modifier = Modifier.fillMaxWidth(),
                                prefix = { Text("₹ ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1976D2))
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = sdfDate.format(Date(paymentDate)),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Date") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = sdfTime.format(Date(paymentDate)),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Time") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            SimpleDropdown(
                                label = "Payment Method *",
                                options = listOf("Cash", "UPI", "Card", "Bank Transfer", "Other"),
                                selected = paymentMethod,
                                onSelect = { paymentMethod = it }
                            )

                            SimpleDropdown(
                                label = "Payment Type *",
                                options = listOf("Advance", "Balance", "Full Payment", "Extra Payment", "Refund"),
                                selected = paymentType,
                                onSelect = { paymentType = it }
                            )
                        }
                    }
                }

                // SECTION: References
                item {
                    PaymentFormSection("Transaction References") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                label = { Text("UPI / Transaction ID") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = receiptNumber,
                                onValueChange = { receiptNumber = it },
                                label = { Text("Receipt Number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = receivedBy,
                                onValueChange = { receivedBy = it },
                                label = { Text("Received By (Staff Name)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // SECTION: Financial Adjustment (Moved from top to allow editing Total/Discount if needed)
                item {
                    PaymentFormSection("Adjustment") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = totalAmount,
                                onValueChange = { if (it.all { char -> char.isDigit() }) totalAmount = it },
                                label = { Text("Total Bill Amount") },
                                modifier = Modifier.fillMaxWidth(),
                                prefix = { Text("₹ ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = discount,
                                onValueChange = { if (it.all { char -> char.isDigit() }) discount = it },
                                label = { Text("Total Discount") },
                                modifier = Modifier.fillMaxWidth(),
                                prefix = { Text("₹ ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun PaymentFormSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1976D2))
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReadOnlyField(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
