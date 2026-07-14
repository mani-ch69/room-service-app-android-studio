package com.example.roomservice.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.TimeZone

@Composable
fun DateDisplayBox(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onClick() }
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
            color = Color.White,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = value, fontSize = 14.sp, color = Color.Black)
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonDateRangePicker(
    state: DateRangePickerState,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(state.selectedStartDateMillis, state.selectedEndDateMillis)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.fillMaxWidth().height(500.dp),
            showModeToggle = false,
            title = { Text("Select Stay Dates", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) },
            headline = {
                val start = state.selectedStartDateMillis
                val end = state.selectedEndDateMillis
                val df = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(if (start != null) df.format(java.util.Date(start)) else "Start Date")
                    Text(" - ")
                    Text(if (end != null) df.format(java.util.Date(end)) else "End Date")
                }
            }
        )
    }
}

object DateRangeUtils {
    fun isSelectableFromToday(utcTimeMillis: Long): Boolean {
        val today = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return utcTimeMillis >= today
    }

    fun getNoonTimestamp(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
