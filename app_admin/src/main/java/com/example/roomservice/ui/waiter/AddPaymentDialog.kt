package com.example.roomservice.ui.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.roomservice.data.model.Booking

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

    val remainingAmount = remember(totalAmount, discount, advancePaid) {
        val total = totalAmount.toDoubleOrNull() ?: 0.0
        val disc = discount.toDoubleOrNull() ?: 0.0
        val adv = advancePaid.toDoubleOrNull() ?: 0.0
        (total - disc - adv).coerceAtLeast(0.0)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, null, tint = Color(0xFF1976D2))
                    Spacer(Modifier.width(12.dp))
                    Text("Add/Adjust Payment", fontWeight = FontWeight.Black, fontSize = 20.sp)
                }

                Divider(color = Color(0xFFF1F5F9))

                // Read-only info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReadOnlyField("Booking ID", booking.bookingNumber)
                    ReadOnlyField("Guest Name", booking.guestName)
                }

                Divider(color = Color(0xFFF1F5F9))

                // Editable fields
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) totalAmount = it },
                    label = { Text("Total Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = discount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) discount = it },
                    label = { Text("Discount") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = advancePaid,
                    onValueChange = { if (it.all { char -> char.isDigit() }) advancePaid = it },
                    label = { Text("Advance Paid") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                // Calculated remaining
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Remaining Amount", fontWeight = FontWeight.Bold)
                        Text("₹ ${remainingAmount.toInt()}", fontWeight = FontWeight.Black, color = Color.Red)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onConfirm(booking.copy(
                                totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                                discount = discount.toDoubleOrNull() ?: 0.0,
                                advancePaid = advancePaid.toDoubleOrNull() ?: 0.0
                            ))
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Text("Update Payment", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReadOnlyField(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
