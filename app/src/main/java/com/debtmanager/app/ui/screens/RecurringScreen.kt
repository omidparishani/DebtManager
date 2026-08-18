package com.debtmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.PaymentFrequency
import com.debtmanager.app.data.entity.RecurringPayment
import com.debtmanager.app.data.repository.getInstallmentStatus
import com.debtmanager.app.ui.components.ActionIconButton
import com.debtmanager.app.ui.components.*
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(viewModel: MainViewModel) {
    val payments by viewModel.recurring.collectAsState()
    val accounts by viewModel.bankAccounts.collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<RecurringPayment?>(null) }
    var deleteTarget by remember { mutableStateOf<RecurringPayment?>(null) }
    var payTarget by remember { mutableStateOf<RecurringPayment?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) { Icon(Icons.Default.Add, "افزودن") }
        }
    ) { padding ->
        if (payments.isEmpty()) {
            EmptyState("قسط دوره‌ای ثبت نشده است", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(payments, key = { it.id }) { payment ->
                    RecurringCard(
                        payment = payment,
                        onMarkPaid = { payTarget = payment },
                        onEdit = { editTarget = payment },
                        onDelete = { deleteTarget = payment }
                    )
                }
            }
        }
    }

    if (showAdd) RecurringFormDialog(null, onDismiss = { showAdd = false }) { payment ->
        viewModel.addRecurring(payment) { showAdd = false }
    }
    editTarget?.let { payment ->
        RecurringFormDialog(payment, onDismiss = { editTarget = null }) { updated ->
            viewModel.updateRecurring(updated) { editTarget = null }
        }
    }
    deleteTarget?.let { payment ->
        ConfirmDialog("حذف", "آیا مطمئن هستید؟",
            onConfirm = { viewModel.deleteRecurring(payment) { deleteTarget = null } },
            onDismiss = { deleteTarget = null })
    }
    payTarget?.let { payment ->
        var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var selectedAccountId by remember {
            mutableStateOf(accounts.find { it.isDefault }?.id ?: accounts.firstOrNull()?.id)
        }
        AlertDialog(
            onDismissRequest = { payTarget = null },
            title = { Text("ثبت پرداخت دوره‌ای") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(payment.title, style = MaterialTheme.typography.titleMedium)
                    Text("مبلغ: ${CurrencyUtil.format(payment.amount)}")
                    JalaliDatePickerField(date, { date = it }, "تاریخ پرداخت")
                    AccountPickerField(accounts, selectedAccountId, { selectedAccountId = it }, "پرداخت از حساب")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markRecurringPaid(payment, date, selectedAccountId) { payTarget = null }
                }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { payTarget = null }) { Text("انصراف") } }
        )
    }
}

@Composable
fun RecurringCard(payment: RecurringPayment, onMarkPaid: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val freq = PaymentFrequency.entries.find { it.name == payment.frequency }?.label ?: payment.frequency
    val status = getInstallmentStatus(payment.nextDueDate, false)

    ElevatedCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            ItemIconBadge(payment.icon.ifBlank { "money" })
            Column(Modifier.weight(1f)) {
                Text(payment.title, fontWeight = FontWeight.Bold)
                AmountText(payment.amount)
                Text("دوره: $freq")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("سررسید بعدی: ")
                    DateText(payment.nextDueDate, short = true)
                }
                if (payment.category.isNotBlank()) Text("دسته: ${payment.category}")
                StatusChip(status)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onMarkPaid) {
                        Icon(Icons.Default.Check, null)
                        Text("پرداخت شد")
                    }
                    ActionIconButton(Icons.Default.Edit, "ویرایش", onEdit)
                    ActionIconButton(Icons.Default.Delete, "حذف", onDelete, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormDialog(existing: RecurringPayment?, onDismiss: () -> Unit, onSave: (RecurringPayment) -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amount by remember {
        mutableStateOf(existing?.amount?.let { CurrencyUtil.formatWithoutUnit(it) } ?: "")
    }
    var frequency by remember { mutableStateOf(existing?.frequency ?: PaymentFrequency.MONTHLY.name) }
    var nextDueDate by remember { mutableLongStateOf(existing?.nextDueDate ?: System.currentTimeMillis()) }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var icon by remember { mutableStateOf(existing?.icon?.ifBlank { "money" } ?: "money") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "افزودن قسط دوره‌ای" else "ویرایش قسط دوره‌ای") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(title, { title = it }, label = { Text("عنوان *") }, modifier = Modifier.fillMaxWidth()) }
                item { AmountTextField(amount, { amount = it }, "مبلغ (ریال) *") }
                item {
                    ExposedDropdownMenuBox(expanded, { expanded = it }) {
                        val label = PaymentFrequency.entries.find { it.name == frequency }?.label ?: ""
                        OutlinedTextField(label, {}, readOnly = true, label = { Text("دوره") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded, { expanded = false }) {
                            PaymentFrequency.entries.forEach { f ->
                                DropdownMenuItem(text = { Text(f.label) }, onClick = { frequency = f.name; expanded = false })
                            }
                        }
                    }
                }
                item { JalaliDatePickerField(nextDueDate, { nextDueDate = it }, "تاریخ بعدی پرداخت") }
                item { OutlinedTextField(category, { category = it }, label = { Text("دسته (مثلاً بیمه)") }, modifier = Modifier.fillMaxWidth()) }
                item { IconPicker(selectedIcon = icon, onIconSelected = { icon = it }, label = "آیکون") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (title.isBlank()) return@TextButton
                onSave(RecurringPayment(
                    id = existing?.id ?: 0,
                    title = title,
                    amount = a,
                    frequency = frequency,
                    nextDueDate = nextDueDate,
                    lastPaidDate = existing?.lastPaidDate,
                    category = category,
                    icon = icon
                ))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
