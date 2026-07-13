package com.debtmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.Debt
import com.debtmanager.app.data.entity.DebtCategory
import com.debtmanager.app.ui.components.*
import com.debtmanager.app.ui.theme.StatusOverdue
import com.debtmanager.app.ui.theme.StatusPaid
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(viewModel: MainViewModel) {
    val debts by viewModel.debts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var payTarget by remember { mutableStateOf<Debt?>(null) }
    var deleteTarget by remember { mutableStateOf<Debt?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("بدهکاری‌ها") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "افزودن") }
        }
    ) { padding ->
        if (debts.isEmpty()) {
            EmptyState("بدهی ثبت نشده است", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(debts, key = { it.id }) { debt ->
                    DebtCard(debt, onPay = { payTarget = debt }, onDelete = { deleteTarget = debt })
                }
            }
        }
    }

    if (showAdd) DebtFormDialog(onDismiss = { showAdd = false }) { debt ->
        viewModel.addDebt(debt) { showAdd = false }
    }
    payTarget?.let { debt -> PayDebtDialog(debt, onDismiss = { payTarget = null }) { amount, date ->
        viewModel.payDebt(debt, amount, date) { payTarget = null }
    }}
    deleteTarget?.let { debt ->
        ConfirmDialog("حذف بدهی", "آیا مطمئن هستید؟",
            onConfirm = { viewModel.deleteDebt(debt) { deleteTarget = null } },
            onDismiss = { deleteTarget = null })
    }
}

@Composable
fun DebtCard(debt: Debt, onPay: () -> Unit, onDelete: () -> Unit) {
    val remaining = debt.totalAmount - debt.paidAmount
    val isPaid = remaining <= 0
    val categoryLabel = DebtCategory.entries.find { it.name == debt.category }?.label ?: debt.category

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(debt.creditorName, fontWeight = FontWeight.Bold)
                Text(categoryLabel, style = MaterialTheme.typography.labelSmall)
            }
            Text("کل: ${CurrencyUtil.format(debt.totalAmount)}")
            Text("پرداخت‌شده: ${CurrencyUtil.format(debt.paidAmount)}", color = StatusPaid)
            Text("مانده: ${CurrencyUtil.format(remaining)}", color = if (isPaid) StatusPaid else StatusOverdue, fontWeight = FontWeight.Bold)
            DateText(debt.date, short = true)
            if (debt.description.isNotBlank()) Text(debt.description, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!isPaid) {
                    TextButton(onClick = onPay) { Icon(Icons.Default.Payment, null); Text("پرداخت") }
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormDialog(onDismiss: () -> Unit, onSave: (Debt) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DebtCategory.PERSON.name) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن بدهی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام فرد/فروشگاه *") }, modifier = Modifier.fillMaxWidth())
                AmountTextField(amount, { amount = it }, "مبلغ (ریال) *")
                JalaliDatePickerField(date, { date = it }, "تاریخ")
                OutlinedTextField(description, { description = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded, { expanded = it }) {
                    val label = DebtCategory.entries.find { it.name == category }?.label ?: ""
                    OutlinedTextField(label, {}, readOnly = true, label = { Text("دسته‌بندی") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        DebtCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.label) }, onClick = { category = cat.name; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (name.isBlank()) return@TextButton
                onSave(Debt(creditorName = name, category = category, totalAmount = a, date = date, description = description))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun PayDebtDialog(debt: Debt, onDismiss: () -> Unit, onPay: (Long, Long) -> Unit) {
    val remaining = debt.totalAmount - debt.paidAmount
    var amount by remember { mutableStateOf(remaining.toString()) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پرداخت بدهی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مانده: ${CurrencyUtil.format(remaining)}")
                AmountTextField(amount, { amount = it }, "مبلغ پرداختی (ریال)")
                JalaliDatePickerField(date, { date = it }, "تاریخ پرداخت")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                onPay(a.coerceAtMost(remaining), date)
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
