package com.debtmanager.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.InstallmentStatus
import com.debtmanager.app.ui.theme.statusColor
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.util.ItemIconOption
import com.debtmanager.app.util.ItemIcons
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
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { showDialog = true }
    ) {
        OutlinedTextField(
            value = PersianDateUtil.formatShort(selectedDate),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.CalendarMonth, contentDescription = "انتخاب تاریخ")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
            )
        )
    }

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
fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    icons: List<ItemIconOption> = ItemIcons.all,
    label: String = "آیکون",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 160.dp)
        ) {
            items(icons) { option ->
                val selected = option.key == selectedIcon
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onIconSelected(option.key) },
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    shadowElevation = if (selected) 4.dp else 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            option.icon,
                            contentDescription = option.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemIconBadge(
    iconKey: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = tint.copy(alpha = 0.12f),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                ItemIcons.resolve(iconKey),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size((size * 0.55f).dp)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Card(
        modifier = modifier.shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            color.copy(alpha = 0.18f),
                            color.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    icon?.let {
                        Icon(it, null, tint = color, modifier = Modifier.size(18.dp))
                    }
                    Text(title, style = MaterialTheme.typography.labelMedium, color = color)
                }
                Spacer(Modifier.height(6.dp))
                Text(value, style = MaterialTheme.typography.titleMedium, color = color)
            }
        }
    }
}

@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(8.dp, RoundedCornerShape(16.dp))
        .clip(RoundedCornerShape(16.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp), content = content)
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
    onDismiss: () -> Unit,
    confirmLabel: String = "بله"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("خیر") }
        }
    )
}
