package com.debtmanager.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.InstallmentStatus
import com.debtmanager.app.ui.theme.statusColor
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.util.PersianDateUtil

@Composable
fun AmountText(amount: Long, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge) {
    Text(
        text = CurrencyUtil.format(amount),
        modifier = modifier,
        style = style,
        textAlign = TextAlign.End
    )
}

@Composable
fun DateText(date: Long, modifier: Modifier = Modifier, short: Boolean = false) {
    Text(
        text = if (short) PersianDateUtil.formatShort(date) else PersianDateUtil.format(date),
        modifier = modifier
    )
}

@Composable
fun StatusChip(status: InstallmentStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        InstallmentStatus.PAID -> "پرداخت‌شده" to statusColor(status)
        InstallmentStatus.UPCOMING -> "آینده" to statusColor(status)
        InstallmentStatus.OVERDUE -> "معوق" to statusColor(status)
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun StatusChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JalaliDatePickerField(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val j = PersianDateUtil.fromTimestamp(selectedDate)

    OutlinedTextField(
        value = PersianDateUtil.formatShort(selectedDate),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().clickable { showDialog = true }
    )

    if (showDialog) {
        JalaliDatePickerDialog(
            initialYear = j.year,
            initialMonth = j.month,
            initialDay = j.day,
            onDismiss = { showDialog = false },
            onConfirm = { y, m, d ->
                onDateSelected(PersianDateUtil.toTimestamp(y, m, d))
                showDialog = false
            }
        )
    }
}

@Composable
fun JalaliDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var year by remember { mutableIntStateOf(initialYear) }
    var month by remember { mutableIntStateOf(initialMonth) }
    var day by remember { mutableIntStateOf(initialDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("سال")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { year++ }) { Text("+") }
                        Text(PersianDateUtil.toPersianDigits(year))
                        TextButton(onClick = { if (year > 1300) year-- }) { Text("-") }
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("ماه")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { if (month < 12) month++ else { month = 1; year++ } }) { Text("+") }
                        Text(PersianDateUtil.monthName(month))
                        TextButton(onClick = { if (month > 1) month-- else { month = 12; year-- } }) { Text("-") }
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("روز")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { day = (day % 31) + 1 }) { Text("+") }
                        Text(PersianDateUtil.toPersianDigits(day))
                        TextButton(onClick = { if (day > 1) day-- else day = 31 }) { Text("-") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(year, month, day) }) { Text("تأیید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("بله") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("خیر") }
        }
    )
}
