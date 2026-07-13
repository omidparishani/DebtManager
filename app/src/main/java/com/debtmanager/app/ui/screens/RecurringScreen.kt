package com.debtmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.PaymentFrequency
import com.debtmanager.app.data.entity.RecurringPayment
import com.debtmanager.app.data.repository.getInstallmentStatus
import com.debtmanager.app.ui.components.*
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(viewModel: MainViewModel) {
    val payments by viewModel.recurring.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<RecurringPayment?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("اقساط دوره‌ای") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "افزودن") }
        }
    ) { padding ->
        if (payments.isEmpty()) {
            EmptyState("قسط دوره‌ای ثبت نشده است", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(payments, key = { it.id }) { payment ->
                    RecurringCard(
                        payment = payment,
                        onMarkPaid = { viewModel.markRecurringPaid(payment, System.currentTimeMillis()) {} },
                        onDelete = { deleteTarget = payment }
                    )
                }
            }
        }
    }

    if (showAdd) RecurringFormDialog(onDismiss = { showAdd = false }) { payment ->
        viewModel.addRecurring(payment) { showAdd = false }
    }
    deleteTarget?.let { payment ->
        ConfirmDialog("حذف", "آیا مطمئن هستید؟",
            onConfirm = { viewModel.deleteRecurring(payment) { deleteTarget = null } },
            onDismiss = { deleteTarget = null })
    }
}

@Composable
fun RecurringCard(payment: RecurringPayment, onMarkPaid: () -> Unit, onDelete: () -> Unit) {
    val freq = PaymentFrequency.entries.find { it.name == payment.frequency }?.label ?: payment.frequency
    val status = getInstallmentStatus(payment.nextDueDate, false)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(payment.title, fontWeight = FontWeight.Bold)
            AmountText(payment.amount)
            Text("دوره: $freq")
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormDialog(onDismiss: () -> Unit, onSave: (RecurringPayment) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(PaymentFrequency.MONTHLY.name) }
    var nextDueDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var category by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن قسط دوره‌ای") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان *") }, modifier = Modifier.fillMaxWidth())
                AmountTextField(amount, { amount = it }, "مبلغ (ریال) *")
                ExposedDropdownMenuBox(expanded, { expanded = it }) {
                    val label = PaymentFrequency.entries.find { it.name == frequency }?.label ?: ""
                    OutlinedTextField(label, {}, readOnly = true, label = { Text("دوره") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        PaymentFrequency.entries.forEach { f ->
                            DropdownMenuItem(text = { Text(f.label) }, onClick = { frequency = f.name; expanded = false })
                        }
                    }
                }
                JalaliDatePickerField(nextDueDate, { nextDueDate = it }, "تاریخ بعدی پرداخت")
                OutlinedTextField(category, { category = it }, label = { Text("دسته (مثلاً بیمه)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (title.isBlank()) return@TextButton
                onSave(RecurringPayment(title = title, amount = a, frequency = frequency, nextDueDate = nextDueDate, category = category))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
