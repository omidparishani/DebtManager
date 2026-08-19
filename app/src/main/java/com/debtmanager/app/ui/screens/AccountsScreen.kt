package com.debtmanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.AccountTransaction
import com.debtmanager.app.data.entity.BankAccount
import com.debtmanager.app.ui.components.ActionIconButton
import com.debtmanager.app.ui.components.AmountTextField
import com.debtmanager.app.ui.components.EmptyState
import com.debtmanager.app.ui.components.ItemIconBadge
import com.debtmanager.app.ui.components.JalaliDatePickerField
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.util.PersianDateUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(viewModel: MainViewModel) {
    val accounts by viewModel.bankAccounts.collectAsState(initial = emptyList())
    val transactions by viewModel.accountTransactions.collectAsState(initial = emptyList())
    var showAddAccount by remember { mutableStateOf(false) }
    var showDeposit by remember { mutableStateOf<BankAccount?>(null) }
    var showWithdraw by remember { mutableStateOf<BankAccount?>(null) }
    var showTransfer by remember { mutableStateOf(false) }
    var detailAccount by remember { mutableStateOf<BankAccount?>(null) }
    var deleteTarget by remember { mutableStateOf<BankAccount?>(null) }
    var editTx by remember { mutableStateOf<AccountTransaction?>(null) }
    var deleteTx by remember { mutableStateOf<AccountTransaction?>(null) }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (accounts.size >= 2) {
                    SmallFloatingActionButton(onClick = { showTransfer = true }) {
                        Icon(Icons.Default.SwapHoriz, "انتقال")
                    }
                }
                FloatingActionButton(onClick = { showAddAccount = true }) {
                    Icon(Icons.Default.Add, "افزودن حساب")
                }
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            EmptyState(
                "هنوز حساب بانکی تعریف نشده.\nبا دکمه + حساب جدید بسازید.",
                Modifier.padding(padding).fillMaxSize()
            )
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("حساب‌های بانکی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "مجموع موجودی: ${CurrencyUtil.format(accounts.sumOf { it.balance })}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(accounts, key = { it.id }) { acc ->
                    ElevatedCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { detailAccount = acc },
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.elevatedCardElevation(3.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ItemIconBadge(acc.icon.ifBlank { "account_balance" })
                                Column(Modifier.weight(1f)) {
                                    Text(acc.name, fontWeight = FontWeight.Bold)
                                    if (acc.bankName.isNotBlank()) {
                                        Text(acc.bankName, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (acc.accountNumber.isNotBlank()) {
                                        Text(acc.accountNumber, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (acc.isDefault) {
                                    AssistChip(onClick = {}, label = { Text("پیش‌فرض") })
                                }
                            }
                            Text(
                                CurrencyUtil.format(acc.balance),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (acc.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = { showDeposit = acc },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("واریز")
                                }
                                OutlinedButton(
                                    onClick = { showWithdraw = acc },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Remove, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("برداشت")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detailAccount?.let { acc ->
        val txs = transactions.filter { it.accountId == acc.id }.sortedByDescending { it.date }
        AlertDialog(
            onDismissRequest = { detailAccount = null },
            title = {
                Column {
                    Text(acc.name)
                    Text(
                        "موجودی: ${CurrencyUtil.format(acc.balance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (acc.bankName.isNotBlank()) {
                        Text(acc.bankName, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "تراکنش‌ها (${PersianDateUtil.toPersianDigits(txs.size)} مورد)",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    if (txs.isEmpty()) {
                        Text("تراکنشی ثبت نشده")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(txs, key = { it.id }) { tx ->
                                val isPositive = tx.type == "DEPOSIT" ||
                                    (tx.type == "TRANSFER" && tx.description.contains("از حساب مبدأ"))
                                val color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                val sign = if (isPositive) "+" else "−"
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .clickable { editTx = tx }
                                        ) {
                                            Text(tx.description.ifBlank { tx.type }, maxLines = 2)
                                            Text(
                                                PersianDateUtil.formatShort(tx.date),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                "$sign ${CurrencyUtil.format(tx.amount)}",
                                                color = color,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        ActionIconButton(Icons.Default.Edit, "ویرایش", { editTx = tx }, size = 28)
                                        ActionIconButton(
                                            Icons.Default.Delete,
                                            "حذف",
                                            { deleteTx = tx },
                                            tint = MaterialTheme.colorScheme.error,
                                            size = 28
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            detailAccount = null
                            deleteTarget = acc
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("حذف حساب")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailAccount = null }) { Text("بستن") }
            }
        )
    }

    editTx?.let { tx ->
        EditTransactionDialog(
            tx = tx,
            onDismiss = { editTx = null },
            onSave = { amount, date, desc ->
                viewModel.updateAccountTransaction(tx, amount, date, desc) {
                    editTx = null
                }
            }
        )
    }

    deleteTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteTx = null },
            title = { Text("حذف تراکنش") },
            text = { Text("با حذف، موجودی حساب هم اصلاح می‌شود. ادامه؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccountTransaction(tx) { deleteTx = null }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTx = null }) { Text("انصراف") } }
        )
    }

    deleteTarget?.let { acc ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف حساب") },
            text = { Text("آیا از حذف «${acc.name}» و تراکنش‌های آن مطمئن هستید؟") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteBankAccount(acc) { deleteTarget = null } },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("انصراف") } }
        )
    }

    if (showAddAccount) {
        AccountFormDialog(onDismiss = { showAddAccount = false }) { acc ->
            viewModel.addBankAccount(acc) { showAddAccount = false }
        }
    }
    showDeposit?.let { acc ->
        TxDialog("واریز به ${acc.name}", onDismiss = { showDeposit = null }) { amount, date, desc ->
            viewModel.deposit(acc.id, amount, date, desc) { showDeposit = null }
        }
    }
    showWithdraw?.let { acc ->
        TxDialog("برداشت از ${acc.name}", onDismiss = { showWithdraw = null }) { amount, date, desc ->
            viewModel.withdraw(acc.id, amount, date, desc) { showWithdraw = null }
        }
    }
    if (showTransfer) {
        TransferDialog(
            accounts = accounts,
            onDismiss = { showTransfer = false },
            onConfirm = { from, to, amount, fee, date, desc ->
                viewModel.transferBetweenAccounts(from, to, amount, fee, date, desc) {
                    showTransfer = false
                }
            }
        )
    }
}

@Composable
fun EditTransactionDialog(
    tx: AccountTransaction,
    onDismiss: () -> Unit,
    onSave: (Long, Long, String) -> Unit
) {
    var amount by remember { mutableStateOf(CurrencyUtil.formatWithoutUnit(tx.amount)) }
    var date by remember { mutableLongStateOf(tx.date) }
    var desc by remember { mutableStateOf(tx.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش تراکنش") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نوع: ${tx.type}", style = MaterialTheme.typography.labelMedium)
                AmountTextField(amount, { amount = it }, "مبلغ (ریال) *")
                JalaliDatePickerField(date, { date = it }, "تاریخ")
                OutlinedTextField(desc, { desc = it }, label = { Text("شرح") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (a <= 0) return@TextButton
                onSave(a, date, desc.trim())
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onConfirm: (fromId: Long, toId: Long, amount: Long, fee: Long, date: Long, desc: String) -> Unit
) {
    var fromId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var toId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var desc by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتقال بین حساب‌ها") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(fromExpanded, { fromExpanded = it }) {
                    val name = accounts.find { it.id == fromId }?.name ?: "انتخاب مبدأ"
                    OutlinedTextField(
                        name, {}, readOnly = true,
                        label = { Text("از حساب *") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(fromExpanded, { fromExpanded = false }) {
                        accounts.forEach { a ->
                            DropdownMenuItem(
                                text = { Text("${a.name} (${CurrencyUtil.format(a.balance)})") },
                                onClick = { fromId = a.id; fromExpanded = false }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(toExpanded, { toExpanded = it }) {
                    val name = accounts.find { it.id == toId }?.name ?: "انتخاب مقصد"
                    OutlinedTextField(
                        name, {}, readOnly = true,
                        label = { Text("به حساب *") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(toExpanded, { toExpanded = false }) {
                        accounts.filter { it.id != fromId }.forEach { a ->
                            DropdownMenuItem(
                                text = { Text("${a.name} (${CurrencyUtil.format(a.balance)})") },
                                onClick = { toId = a.id; toExpanded = false }
                            )
                        }
                    }
                }
                AmountTextField(amount, { amount = it }, "مبلغ انتقال (ریال) *")
                AmountTextField(fee, { fee = it }, "کارمزد (ریال)")
                JalaliDatePickerField(date, { date = it }, "تاریخ")
                OutlinedTextField(desc, { desc = it }, label = { Text("شرح") }, modifier = Modifier.fillMaxWidth())
                val a = CurrencyUtil.parse(amount) ?: 0L
                val f = CurrencyUtil.parse(fee) ?: 0L
                if (a > 0) {
                    Text(
                        "از مبدأ کسر می‌شود: ${CurrencyUtil.format(a + f)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                val f = CurrencyUtil.parse(fee) ?: 0L
                val from = fromId ?: return@TextButton
                val to = toId ?: return@TextButton
                if (a <= 0 || from == to) return@TextButton
                onConfirm(from, to, a, f, date, desc.trim())
            }) { Text("انتقال") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun AccountFormDialog(onDismiss: () -> Unit, onSave: (BankAccount) -> Unit) {
    var name by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن حساب بانکی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام حساب *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(bankName, { bankName = it }, label = { Text("نام بانک") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(accountNumber, { accountNumber = it }, label = { Text("شماره حساب") }, modifier = Modifier.fillMaxWidth())
                AmountTextField(balance, { balance = it }, "موجودی اولیه (ریال)")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(isDefault, { isDefault = it })
                    Text("حساب پیش‌فرض")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) return@TextButton
                val bal = CurrencyUtil.parse(balance) ?: 0L
                onSave(
                    BankAccount(
                        name = name.trim(),
                        bankName = bankName.trim(),
                        accountNumber = accountNumber.trim(),
                        balance = bal,
                        isDefault = isDefault
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun TxDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountTextField(amount, { amount = it }, "مبلغ (ریال) *")
                JalaliDatePickerField(date, { date = it }, "تاریخ")
                OutlinedTextField(desc, { desc = it }, label = { Text("شرح / توضیحات") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (a <= 0) return@TextButton
                onConfirm(a, date, desc.trim())
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
