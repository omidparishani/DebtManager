package com.debtmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.Debt
import com.debtmanager.app.data.entity.DebtCategory
import com.debtmanager.app.ui.components.*
import com.debtmanager.app.ui.theme.StatusOverdue
import com.debtmanager.app.ui.theme.StatusPaid
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.util.ItemIcons
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(viewModel: MainViewModel) {
    val debts by viewModel.debts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Debt?>(null) }
    var payTarget by remember { mutableStateOf<Debt?>(null) }
    var deleteTarget by remember { mutableStateOf<Debt?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) { Icon(Icons.Default.Add, "افزودن") }
        }
    ) { padding ->
        if (debts.isEmpty()) {
            EmptyState("بدهی ثبت نشده است", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(debts, key = { it.id }) { debt ->
                    DebtCard(
                        debt,
                        onPay = { payTarget = debt },
                        onEdit = { editTarget = debt },
                        onDelete = { deleteTarget = debt }
                    )
                }
            }
        }
    }

    if (showAdd) DebtFormDialog(null, onDismiss = { showAdd = false }) { debt ->
        viewModel.addDebt(debt) { showAdd = false }
    }
    editTarget?.let { debt ->
        DebtFormDialog(debt, onDismiss = { editTarget = null }) { updated ->
            viewModel.updateDebt(updated) { editTarget = null }
        }
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
fun DebtCard(debt: Debt, onPay: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val remaining = debt.totalAmount - debt.paidAmount
    val isPaid = remaining <= 0
    val categoryLabel = DebtCategory.entries.find { it.name == debt.category }?.label ?: debt.category
    val iconKey = debt.icon.ifBlank { ItemIcons.defaultForDebtCategory(debt.category) }

    ElevatedCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            ItemIconBadge(iconKey, tint = if (isPaid) StatusPaid else StatusOverdue)
            Column(Modifier.weight(1f)) {
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
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "ویرایش") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormDialog(existing: Debt?, onDismiss: () -> Unit, onSave: (Debt) -> Unit) {
    var name by remember { mutableStateOf(existing?.creditorName ?: "") }
    var amount by remember {
        mutableStateOf(existing?.totalAmount?.let { CurrencyUtil.formatWithoutUnit(it) } ?: "")
    }
    var date by remember { mutableLongStateOf(existing?.date ?: System.currentTimeMillis()) }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: DebtCategory.PERSON.name) }
    var icon by remember {
        mutableStateOf(
            existing?.let { it.icon.ifBlank { ItemIcons.defaultForDebtCategory(it.category) } }
                ?: ItemIcons.defaultForDebtCategory(DebtCategory.PERSON.name)
        )
    }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "افزودن بدهی" else "ویرایش بدهی") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("نام فرد/فروشگاه *") }, modifier = Modifier.fillMaxWidth()) }
                item { AmountTextField(amount, { amount = it }, "مبلغ (ریال) *") }
                item { JalaliDatePickerField(date, { date = it }, "تاریخ") }
                item { OutlinedTextField(description, { description = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    ExposedDropdownMenuBox(expanded, { expanded = it }) {
                        val label = DebtCategory.entries.find { it.name == category }?.label ?: ""
                        OutlinedTextField(label, {}, readOnly = true, label = { Text("دسته‌بندی") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded, { expanded = false }) {
                            DebtCategory.entries.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat.label) }, onClick = {
                                    category = cat.name
                                    if (icon.isBlank() || icon == ItemIcons.defaultForDebtCategory(category)) {
                                        icon = ItemIcons.defaultForDebtCategory(cat.name)
                                    }
                                    expanded = false
                                })
                            }
                        }
                    }
                }
                item {
                    IconPicker(
                        selectedIcon = icon,
                        onIconSelected = { icon = it },
                        label = "آیکون"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (name.isBlank()) return@TextButton
                onSave(Debt(
                    id = existing?.id ?: 0,
                    creditorName = name,
                    category = category,
                    totalAmount = a,
                    paidAmount = existing?.paidAmount ?: 0,
                    date = date,
                    description = description,
                    icon = icon
                ))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun PayDebtDialog(debt: Debt, onDismiss: () -> Unit, onPay: (Long, Long) -> Unit) {
    val remaining = debt.totalAmount - debt.paidAmount
    var amount by remember { mutableStateOf(CurrencyUtil.formatWithoutUnit(remaining)) }
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
