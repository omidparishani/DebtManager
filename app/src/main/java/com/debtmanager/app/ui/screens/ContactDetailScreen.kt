package com.debtmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.debtmanager.app.data.entity.Debt
import com.debtmanager.app.ui.components.AccountPickerField
import com.debtmanager.app.ui.components.ActionIconButton
import com.debtmanager.app.ui.components.AmountTextField
import com.debtmanager.app.ui.components.JalaliDatePickerField
import com.debtmanager.app.ui.components.SecondaryTopBar
import com.debtmanager.app.ui.theme.StatusOverdue
import com.debtmanager.app.ui.theme.StatusPaid
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(viewModel: MainViewModel, contactId: Long, navController: NavController) {
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    val debts by viewModel.debts.collectAsState()
    val accounts by viewModel.bankAccounts.collectAsState(initial = emptyList())
    val contact = contacts.find { it.id == contactId }
    val related = remember(debts, contactId, contact) {
        debts.filter { d ->
            d.contactId == contactId || (contact != null && d.creditorName == contact.name)
        }
    }
    val totalDebt = related.filter { !it.isCredit }.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0) }
    val totalCredit = related.filter { it.isCredit }.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0) }
    val net = totalCredit - totalDebt

    var showAdd by remember { mutableStateOf(false) }
    var addAsCredit by remember { mutableStateOf(false) }
    var payTarget by remember { mutableStateOf<Debt?>(null) }
    var editTarget by remember { mutableStateOf<Debt?>(null) }
    var deleteTarget by remember { mutableStateOf<Debt?>(null) }
    var showPayAll by remember { mutableStateOf(false) }
    var payAllMessage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        SecondaryTopBar(
            title = contact?.name ?: "حساب شخص",
            onBack = { navController.popBackStack() }
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("خلاصه حساب", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("بدهکاری شما", style = MaterialTheme.typography.bodySmall)
                                Text(CurrencyUtil.format(totalDebt), color = StatusOverdue, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("بستانکاری شما", style = MaterialTheme.typography.bodySmall)
                                Text(CurrencyUtil.format(totalCredit), color = StatusPaid, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مانده خالص", fontWeight = FontWeight.Medium)
                            Text(
                                when {
                                    net > 0 -> "طلب شما: ${CurrencyUtil.format(net)}"
                                    net < 0 -> "بدهی شما: ${CurrencyUtil.format(-net)}"
                                    else -> "تسویه"
                                },
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    net > 0 -> StatusPaid
                                    net < 0 -> StatusOverdue
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        if (!contact?.phone.isNullOrBlank()) {
                            Text("تلفن: ${contact?.phone}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { addAsCredit = false; showAdd = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("＋ بدهکاری") }
                    Button(
                        onClick = { addAsCredit = true; showAdd = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("＋ بستانکاری") }
                }
                if (totalDebt > 0 && accounts.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showPayAll = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue)
                    ) {
                        Text("تسویه کامل بدهی‌ها (${CurrencyUtil.format(totalDebt)})")
                    }
                }
            }
            item {
                Text("ریز تراکنش‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (related.isEmpty()) {
                item { Text("هنوز موردی ثبت نشده", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(related, key = { it.id }) { debt ->
                    val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0)
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (debt.isCredit) "بستانکاری" else "بدهکاری",
                                        color = if (debt.isCredit) StatusPaid else StatusOverdue,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    if (debt.description.isNotBlank()) Text(debt.description)
                                    Text("کل: ${CurrencyUtil.format(debt.totalAmount)}")
                                    Text(
                                        "پرداخت‌شده: ${CurrencyUtil.format(debt.paidAmount)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text("مانده: ${CurrencyUtil.format(remaining)}", fontWeight = FontWeight.Bold)
                                }
                                Row {
                                    ActionIconButton(Icons.Default.Edit, "ویرایش", { editTarget = debt }, size = 32)
                                    ActionIconButton(
                                        Icons.Default.Delete, "حذف",
                                        { deleteTarget = debt },
                                        tint = MaterialTheme.colorScheme.error,
                                        size = 32
                                    )
                                }
                            }
                            if (remaining > 0) {
                                if (!debt.isCredit) {
                                    Button(
                                        onClick = { payTarget = debt },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Payment, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("پرداخت از حساب بانکی")
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = { payTarget = debt },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Payment, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("دریافت و واریز به حساب")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd && contact != null) {
        val contactsList by viewModel.contacts.collectAsState(initial = emptyList())
        ContactDebtFormDialog(
            isCredit = addAsCredit,
            contactName = contact.name,
            contactId = contact.id,
            accounts = accounts,
            onDismiss = { showAdd = false },
            onSave = { debt, accountId, applyToBank ->
                viewModel.addDebt(
                    debt.copy(
                        creditorName = contact.name,
                        contactId = contact.id,
                        isCredit = addAsCredit
                    )
                ) {
                    if (applyToBank && accountId != null && debt.totalAmount > 0) {
                        // بستانکاری: واریز به حساب | بدهکاری: فعلاً فقط ثبت بدون برداشت تا پرداخت
                        if (addAsCredit) {
                            viewModel.deposit(
                                accountId,
                                debt.totalAmount,
                                debt.date,
                                "بستانکاری از ${contact.name}: ${debt.description}"
                            ) { showAdd = false }
                        } else {
                            showAdd = false
                        }
                    } else {
                        showAdd = false
                    }
                }
            }
        )
    }

    payTarget?.let { debt ->
        PersonPayDialog(
            debt = debt,
            accounts = accounts,
            onDismiss = { payTarget = null },
            onConfirm = { amount, date, accountId ->
                if (debt.isCredit) {
                    // دریافت طلب = پرداخت روی debt + واریز به حساب
                    viewModel.payDebt(debt, amount, date, null) {
                        if (accountId != null) {
                            viewModel.deposit(
                                accountId, amount, date,
                                "دریافت از ${contact?.name ?: debt.creditorName}"
                            ) { payTarget = null }
                        } else {
                            payTarget = null
                        }
                    }
                } else {
                    viewModel.payDebt(debt, amount, date, accountId) { payTarget = null }
                }
            }
        )
    }


    editTarget?.let { debt ->
        ContactDebtFormDialog(
            isCredit = debt.isCredit,
            contactName = contact?.name ?: debt.creditorName,
            contactId = contactId,
            accounts = accounts,
            existing = debt,
            onDismiss = { editTarget = null },
            onSave = { updated, _, _ ->
                viewModel.updateDebt(updated) { editTarget = null }
            }
        )
    }
    deleteTarget?.let { debt ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف تراکنش") },
            text = {
                Text(
                    "آیا از حذف این ${if (debt.isCredit) "بستانکاری" else "بدهکاری"} " +
                        "(${CurrencyUtil.format(debt.totalAmount)}) مطمئن هستید؟"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteDebt(debt) { deleteTarget = null } },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("انصراف") } }
        )
    }

if (showPayAll && contact != null) {
        var accountId by remember {
            mutableStateOf(accounts.find { it.isDefault }?.id ?: accounts.firstOrNull()?.id)
        }
        var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
        AlertDialog(
            onDismissRequest = { showPayAll = false },
            title = { Text("تسویه کامل بدهی‌ها") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مبلغ کل بدهی به ${contact.name}:")
                    Text(CurrencyUtil.format(totalDebt), fontWeight = FontWeight.Bold, color = StatusOverdue)
                    Text("همه بدهکاری‌های باز این شخص یکجا از حساب انتخابی پرداخت می‌شود.")
                    AccountPickerField(
                        accounts = accounts,
                        selectedAccountId = accountId,
                        onAccountSelected = { accountId = it },
                        label = "پرداخت از حساب *"
                    )
                    JalaliDatePickerField(date, { date = it }, "تاریخ")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val accId = accountId ?: return@TextButton
                    viewModel.payAllDebtsForContact(
                        contactId = contact.id,
                        contactName = contact.name,
                        bankAccountId = accId,
                        date = date
                    ) { count, total ->
                        showPayAll = false
                        payAllMessage = "تعداد $count مورد به مبلغ ${CurrencyUtil.format(total)} تسویه شد"
                    }
                }) { Text("تسویه کامل") }
            },
            dismissButton = { TextButton(onClick = { showPayAll = false }) { Text("انصراف") } }
        )
    }
    payAllMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { payAllMessage = null },
            title = { Text("انجام شد") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { payAllMessage = null }) { Text("باشه") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDebtFormDialog(
    isCredit: Boolean,
    contactName: String,
    contactId: Long,
    accounts: List<com.debtmanager.app.data.entity.BankAccount>,
    existing: Debt? = null,
    onDismiss: () -> Unit,
    onSave: (Debt, Long?, Boolean) -> Unit
) {
    var amount by remember {
        mutableStateOf(existing?.totalAmount?.let { CurrencyUtil.formatWithoutUnit(it) } ?: "")
    }
    var date by remember { mutableLongStateOf(existing?.date ?: System.currentTimeMillis()) }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var accountId by remember {
        mutableStateOf(accounts.find { it.isDefault }?.id ?: accounts.firstOrNull()?.id)
    }
    var linkBank by remember { mutableStateOf(existing == null && isCredit && accounts.isNotEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when { existing != null && isCredit -> "ویرایش بستانکاری"; existing != null -> "ویرایش بدهکاری"; isCredit -> "افزودن بستانکاری"; else -> "افزودن بدهکاری" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("شخص: $contactName", fontWeight = FontWeight.Medium)
                AmountTextField(amount, { amount = it }, "مبلغ (ریال) *")
                JalaliDatePickerField(date, { date = it }, "تاریخ")
                OutlinedTextField(
                    description, { description = it },
                    label = { Text("توضیحات") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (accounts.isNotEmpty()) {
                    if (isCredit) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(linkBank, { linkBank = it })
                            Text("واریز هم‌زمان به حساب بانکی")
                        }
                        if (linkBank) {
                            AccountPickerField(
                                accounts = accounts,
                                selectedAccountId = accountId,
                                onAccountSelected = { accountId = it },
                                label = "واریز به حساب"
                            )
                        }
                    } else {
                        Text(
                            "برای پرداخت بدهی بعداً از دکمه «پرداخت از حساب بانکی» استفاده کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (a <= 0) return@TextButton
                onSave(
                    Debt(
                        id = existing?.id ?: 0,
                        creditorName = contactName,
                        category = existing?.category ?: "PERSON",
                        totalAmount = a,
                        paidAmount = existing?.paidAmount ?: 0,
                        date = date,
                        description = description.trim(),
                        isCredit = isCredit,
                        contactId = contactId,
                        icon = existing?.icon ?: ""
                    ),
                    accountId,
                    linkBank && isCredit && existing == null
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun PersonPayDialog(
    debt: Debt,
    accounts: List<com.debtmanager.app.data.entity.BankAccount>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, date: Long, accountId: Long?) -> Unit
) {
    val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0)
    var amount by remember { mutableStateOf(CurrencyUtil.formatWithoutUnit(remaining)) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var accountId by remember {
        mutableStateOf(accounts.find { it.isDefault }?.id ?: accounts.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt.isCredit) "دریافت طلب" else "پرداخت بدهی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مانده: ${CurrencyUtil.format(remaining)}")
                AmountTextField(amount, { amount = it }, "مبلغ *")
                JalaliDatePickerField(date, { date = it }, "تاریخ")
                if (accounts.isNotEmpty()) {
                    AccountPickerField(
                        accounts = accounts,
                        selectedAccountId = accountId,
                        onAccountSelected = { accountId = it },
                        label = if (debt.isCredit) "واریز به حساب *" else "پرداخت از حساب *"
                    )
                } else {
                    Text(
                        "ابتدا یک حساب بانکی تعریف کنید.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (a <= 0) return@TextButton
                if (accounts.isNotEmpty() && accountId == null) return@TextButton
                onConfirm(a, date, accountId)
            }) { Text("تأیید") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
