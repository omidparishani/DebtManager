package com.debtmanager.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.Expense
import com.debtmanager.app.data.entity.ExpenseCategory
import com.debtmanager.app.data.entity.ExpenseMember
import com.debtmanager.app.ui.components.ActionIconButton
import com.debtmanager.app.ui.components.*
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.util.PersianDateUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: MainViewModel) {
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val accounts by viewModel.bankAccounts.collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Expense?>(null) }
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }
    var filterMember by remember { mutableStateOf<String?>(null) }

    val filtered = remember(expenses, filterMember) {
        if (filterMember == null) expenses else expenses.filter { it.member == filterMember }
    }
    val totalFiltered = filtered.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, "افزودن مخارج")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMember == null,
                    onClick = { filterMember = null },
                    label = { Text("همه") }
                )
                ExpenseMember.entries.forEach { m ->
                    FilterChip(
                        selected = filterMember == m.name,
                        onClick = { filterMember = if (filterMember == m.name) null else m.name },
                        label = { Text(m.label) }
                    )
                }
            }
            Text(
                "جمع: ${CurrencyUtil.format(totalFiltered)}",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (filtered.isEmpty()) {
                EmptyState("مخارجی ثبت نشده است", Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { exp ->
                        ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                ItemIconBadge(exp.icon.ifBlank { "shopping_cart" })
                                Column(Modifier.weight(1f)) {
                                    Text(exp.title, fontWeight = FontWeight.Bold)
                                    Text(
                                        CurrencyUtil.format(exp.amount),
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val cat = ExpenseCategory.entries.find { it.name == exp.category }?.label ?: exp.category
                                    val mem = ExpenseMember.entries.find { it.name == exp.member }?.label ?: exp.member
                                    Text("$cat • $mem", style = MaterialTheme.typography.labelSmall)
                                    Text(PersianDateUtil.formatShort(exp.date), style = MaterialTheme.typography.labelSmall)
                                    if (exp.notes.isNotBlank()) {
                                        Text(exp.notes, style = MaterialTheme.typography.bodySmall)
                                    }
                                    val accName = accounts.find { it.id == exp.accountId }?.name
                                    if (accName != null) {
                                        Text("از: $accName", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Column {
                                    ActionIconButton(Icons.Default.Edit, "ویرایش", { editTarget = exp })
                                    ActionIconButton(Icons.Default.Delete, "حذف", { deleteTarget = exp }, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ExpenseFormDialog(null, accounts, onDismiss = { showAdd = false }) { e ->
            viewModel.addExpense(e) { showAdd = false }
        }
    }
    editTarget?.let { e ->
        ExpenseFormDialog(e, accounts, onDismiss = { editTarget = null }) { updated ->
            viewModel.updateExpense(updated) { editTarget = null }
        }
    }
    deleteTarget?.let { e ->
        ConfirmDialog(
            "حذف مخارج",
            "آیا مطمئن هستید؟",
            onConfirm = { viewModel.deleteExpense(e) { deleteTarget = null } },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    existing: Expense?,
    accounts: List<com.debtmanager.app.data.entity.BankAccount>,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amount by remember {
        mutableStateOf(existing?.amount?.let { CurrencyUtil.formatWithoutUnit(it) } ?: "")
    }
    var date by remember { mutableLongStateOf(existing?.date ?: System.currentTimeMillis()) }
    var category by remember { mutableStateOf(existing?.category ?: ExpenseCategory.FOOD.name) }
    var member by remember { mutableStateOf(existing?.member ?: ExpenseMember.SELF.name) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var accountId by remember {
        mutableStateOf(existing?.accountId ?: accounts.find { it.isDefault }?.id ?: accounts.firstOrNull()?.id)
    }
    var catExpanded by remember { mutableStateOf(false) }
    var memExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ثبت مخارج" else "ویرایش مخارج") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(title, { title = it }, label = { Text("عنوان *") }, modifier = Modifier.fillMaxWidth())
                }
                item { AmountTextField(amount, { amount = it }, "مبلغ (ریال) *") }
                item { JalaliDatePickerField(date, { date = it }, "تاریخ") }
                item {
                    ExposedDropdownMenuBox(catExpanded, { catExpanded = it }) {
                        val label = ExpenseCategory.entries.find { it.name == category }?.label ?: ""
                        OutlinedTextField(
                            label, {}, readOnly = true, label = { Text("دسته‌بندی") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(catExpanded, { catExpanded = false }) {
                            ExpenseCategory.entries.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.label) },
                                    onClick = { category = c.name; catExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(memExpanded, { memExpanded = it }) {
                        val label = ExpenseMember.entries.find { it.name == member }?.label ?: ""
                        OutlinedTextField(
                            label, {}, readOnly = true, label = { Text("مربوط به") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(memExpanded, { memExpanded = false }) {
                            ExpenseMember.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.label) },
                                    onClick = { member = m.name; memExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    AccountPickerField(
                        accounts = accounts,
                        selectedAccountId = accountId,
                        onAccountSelected = { accountId = it },
                        label = "پرداخت از حساب"
                    )
                }
                item {
                    OutlinedTextField(notes, { notes = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (title.isBlank() || a <= 0) return@TextButton
                onSave(
                    Expense(
                        id = existing?.id ?: 0,
                        title = title.trim(),
                        amount = a,
                        date = date,
                        category = category,
                        member = member,
                        accountId = accountId,
                        notes = notes.trim(),
                        icon = when (category) {
                            "FOOD" -> "restaurant"
                            "TRANSPORT" -> "directions_car"
                            "HOME" -> "home"
                            "HEALTH" -> "local_hospital"
                            "EDUCATION" -> "school"
                            "CLOTHES" -> "checkroom"
                            "ENTERTAINMENT" -> "sports_esports"
                            "SHOPPING" -> "shopping_cart"
                            else -> "money"
                        }
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
