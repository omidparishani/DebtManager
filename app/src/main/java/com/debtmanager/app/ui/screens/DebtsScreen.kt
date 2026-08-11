@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import com.debtmanager.app.data.entity.BankAccount
import com.debtmanager.app.data.entity.Contact
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
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    val accounts by viewModel.bankAccounts.collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Debt?>(null) }
    var payTarget by remember { mutableStateOf<Debt?>(null) }
    var deleteTarget by remember { mutableStateOf<Debt?>(null) }
    var filterCredit by remember { mutableStateOf<Boolean?>(null) } // null=all, false=debt, true=credit

    val filtered = remember(debts, filterCredit) {
        when (filterCredit) {
            true -> debts.filter { it.isCredit }
            false -> debts.filter { !it.isCredit }
            null -> debts
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) { Icon(Icons.Default.Add, "افزودن") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Filter chips
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filterCredit == null, onClick = { filterCredit = null }, label = { Text("همه") })
                FilterChip(selected = filterCredit == false, onClick = { filterCredit = false }, label = { Text("بدهکاری") })
                FilterChip(selected = filterCredit == true, onClick = { filterCredit = true }, label = { Text("بستانکاری") })
            }
            if (filtered.isEmpty()) {
                EmptyState("موردی ثبت نشده است", Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { debt ->
                        DebtCard(
                            debt = debt,
                            contactName = contacts.find { it.id == debt.contactId }?.name,
                            onPay = { payTarget = debt },
                            onEdit = { editTarget = debt },
                            onDelete = { deleteTarget = debt }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        DebtFormDialog(
            existing = null,
            contacts = contacts,
            onDismiss = { showAdd = false },
            onSave = { debt -> viewModel.addDebt(debt) { showAdd = false } },
            onAddContact = { c, done -> viewModel.addContact(c, done) }
        )
    }
    editTarget?.let { debt ->
        DebtFormDialog(
            existing = debt,
            contacts = contacts,
            onDismiss = { editTarget = null },
            onSave = { updated -> viewModel.updateDebt(updated) { editTarget = null } },
            onAddContact = { c, done -> viewModel.addContact(c, done) }
        )
    }
    payTarget?.let { debt ->
        PayDebtDialog(
            debt = debt,
            accounts = accounts,
            onDismiss = { payTarget = null },
            onPay = { amount, date, accountId ->
                viewModel.payDebt(debt, amount, date, accountId) { payTarget = null }
            }
        )
    }
    deleteTarget?.let { debt ->
        ConfirmDialog(
            "حذف",
            "آیا مطمئن هستید؟",
            onConfirm = { viewModel.deleteDebt(debt) { deleteTarget = null } },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
fun DebtCard(
    debt: Debt,
    contactName: String?,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val remaining = debt.totalAmount - debt.paidAmount
    val isPaid = remaining <= 0
    val categoryLabel = DebtCategory.entries.find { it.name == debt.category }?.label ?: debt.category
    val iconKey = debt.icon.ifBlank { ItemIcons.defaultForDebtCategory(debt.category) }
    val typeLabel = if (debt.isCredit) "بستانکاری" else "بدهکاری"
    val typeColor = if (debt.isCredit) StatusPaid else StatusOverdue

    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            ItemIconBadge(iconKey, tint = if (isPaid) StatusPaid else typeColor)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(debt.creditorName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    AssistChip(
                        onClick = {},
                        label = { Text(typeLabel, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = typeColor)
                    )
                }
                if (!contactName.isNullOrBlank() && contactName != debt.creditorName) {
                    Text("شخص: $contactName", style = MaterialTheme.typography.bodySmall)
                }
                Text("دسته: $categoryLabel", style = MaterialTheme.typography.labelSmall)
                Text("کل: ${CurrencyUtil.format(debt.totalAmount)}")
                Text("پرداخت‌شده: ${CurrencyUtil.format(debt.paidAmount)}", color = StatusPaid)
                Text(
                    "مانده: ${CurrencyUtil.format(remaining)}",
                    color = if (isPaid) StatusPaid else StatusOverdue,
                    fontWeight = FontWeight.Bold
                )
                DateText(debt.date, short = true)
                if (debt.description.isNotBlank()) {
                    Text(debt.description, style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (!isPaid) {
                        TextButton(onClick = onPay) {
                            Icon(Icons.Default.Payment, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("پرداخت")
                        }
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
fun DebtFormDialog(
    existing: Debt?,
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onSave: (Debt) -> Unit,
    onAddContact: (Contact, () -> Unit) -> Unit
) {
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
    var isCredit by remember { mutableStateOf(existing?.isCredit ?: false) }
    var selectedContactId by remember { mutableStateOf(existing?.contactId) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var contactExpanded by remember { mutableStateOf(false) }
    var showQuickContact by remember { mutableStateOf(false) }
    var quickContactName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "افزودن بدهی / بستانکاری" else "ویرایش") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    // نوع: بدهکاری یا بستانکاری
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = !isCredit,
                            onClick = { isCredit = false },
                            label = { Text("بدهکاری (من بدهکارم)") }
                        )
                        FilterChip(
                            selected = isCredit,
                            onClick = { isCredit = true },
                            label = { Text("بستانکاری (طلب من)") }
                        )
                    }
                }
                item {
                    // انتخاب شخص
                    ExposedDropdownMenuBox(contactExpanded, { contactExpanded = it }) {
                        val selectedName = contacts.find { it.id == selectedContactId }?.name
                            ?: if (selectedContactId == null) "بدون شخص / دستی" else name
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("شخص مرتبط") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(contactExpanded) }
                        )
                        ExposedDropdownMenu(contactExpanded, { contactExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("بدون شخص / وارد کردن دستی") },
                                onClick = {
                                    selectedContactId = null
                                    contactExpanded = false
                                }
                            )
                            contacts.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        selectedContactId = c.id
                                        name = c.name
                                        contactExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("＋ افزودن شخص جدید") },
                                onClick = {
                                    contactExpanded = false
                                    showQuickContact = true
                                }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام فرد / فروشگاه *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { AmountTextField(amount, { amount = it }, "مبلغ (ریال) *") }
                item { JalaliDatePickerField(date, { date = it }, "تاریخ") }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("توضیحات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    ExposedDropdownMenuBox(categoryExpanded, { categoryExpanded = it }) {
                        val label = DebtCategory.entries.find { it.name == category }?.label ?: ""
                        OutlinedTextField(
                            value = label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("دسته‌بندی") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(categoryExpanded, { categoryExpanded = false }) {
                            DebtCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.label) },
                                    onClick = {
                                        category = cat.name
                                        if (icon.isBlank() || icon == ItemIcons.defaultForDebtCategory(category)) {
                                            icon = ItemIcons.defaultForDebtCategory(cat.name)
                                        }
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    IconPicker(selectedIcon = icon, onIconSelected = { icon = it }, label = "آیکون")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (name.isBlank()) return@TextButton
                onSave(
                    Debt(
                        id = existing?.id ?: 0,
                        creditorName = name.trim(),
                        category = category,
                        totalAmount = a,
                        paidAmount = existing?.paidAmount ?: 0,
                        date = date,
                        description = description.trim(),
                        icon = icon,
                        isCredit = isCredit,
                        contactId = selectedContactId
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )

    if (showQuickContact) {
        AlertDialog(
            onDismissRequest = { showQuickContact = false },
            title = { Text("افزودن سریع شخص") },
            text = {
                OutlinedTextField(
                    value = quickContactName,
                    onValueChange = { quickContactName = it },
                    label = { Text("نام شخص *") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (quickContactName.isBlank()) return@TextButton
                    val c = Contact(name = quickContactName.trim())
                    onAddContact(c) {
                        // after insert, contacts flow will update; user can re-select
                        name = quickContactName.trim()
                        showQuickContact = false
                        quickContactName = ""
                    }
                }) { Text("افزودن") }
            },
            dismissButton = { TextButton(onClick = { showQuickContact = false }) { Text("انصراف") } }
        )
    }
}

@Composable
fun PayDebtDialog(
    debt: Debt,
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onPay: (amount: Long, date: Long, accountId: Long?) -> Unit
) {
    val remaining = debt.totalAmount - debt.paidAmount
    var amount by remember { mutableStateOf(CurrencyUtil.formatWithoutUnit(remaining)) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedAccountId by remember {
        mutableStateOf(accounts.find { it.isDefault }?.id ?: accounts.firstOrNull()?.id)
    }
    var accountExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt.isCredit) "دریافت بستانکاری" else "پرداخت بدهی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("مانده: ${CurrencyUtil.format(remaining)}", fontWeight = FontWeight.Bold)
                AmountTextField(amount, { amount = it }, "مبلغ (ریال)")
                JalaliDatePickerField(date, { date = it }, "تاریخ")
                if (accounts.isNotEmpty()) {
                    ExposedDropdownMenuBox(accountExpanded, { accountExpanded = it }) {
                        val accName = accounts.find { it.id == selectedAccountId }?.let {
                            "${it.name} (${CurrencyUtil.formatWithoutUnit(it.balance)})"
                        } ?: "انتخاب حساب"
                        OutlinedTextField(
                            value = accName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (debt.isCredit) "واریز به حساب" else "پرداخت از حساب") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) }
                        )
                        ExposedDropdownMenu(accountExpanded, { accountExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("بدون حساب") },
                                onClick = { selectedAccountId = null; accountExpanded = false }
                            )
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} — ${CurrencyUtil.format(acc.balance)}") },
                                    onClick = { selectedAccountId = acc.id; accountExpanded = false }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "حساب بانکی تعریف نشده. از بخش حساب‌ها اضافه کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (a <= 0) return@TextButton
                onPay(a, date, selectedAccountId)
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
