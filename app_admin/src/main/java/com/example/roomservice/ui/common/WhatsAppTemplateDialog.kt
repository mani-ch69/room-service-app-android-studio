package com.example.roomservice.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomservice.data.WhatsAppTemplateRepository
import com.example.roomservice.data.model.Booking
import com.example.roomservice.data.model.BusinessDetails
import com.example.roomservice.util.TemplateEngine

@Composable
fun WhatsAppTemplateDialog(
    booking: Booking,
    business: BusinessDetails?,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    val context = LocalContext.current
    val templates = remember { WhatsAppTemplateRepository.getTemplates(context) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Message Template", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(templates) { template ->
                    Card(
                        onClick = {
                            val resolved = TemplateEngine.resolve(template.content, booking, business)
                            onSend(resolved)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(template.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                template.content, 
                                fontSize = 11.sp, 
                                color = Color.Gray,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
