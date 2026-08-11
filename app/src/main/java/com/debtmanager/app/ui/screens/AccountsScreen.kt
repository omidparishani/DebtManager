package com.debtmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.AccountTransaction
import com.debtmanager.app.data.entity.BankAccount
import com.debtmanager.app.ui.components.AmountTextField
import com.debtmanager.app.ui.components.EmptyState
import com.debtmanager.app.ui.components.ItemIconBadge
import com.debtmanager.app.ui.components.JalaliDatePickerField
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(viewModel: MainViewModel) {
    val accounts by viewModel.bankAccounts.collectAsState(initial = emptyList())
    val transactions by viewModel.accountTransactions.collectAsState(initial = emptyList())
    var showAddAccount by remember { mutableStateOf(false) }
    var showDeposit by remember { mutableStateOf<BankAccount?>(null) }
    var showWithdraw by remember { mutableStateOf<BankAccount?>(null) }
    var selectedAccountForReport by remember { mutableStateOf<BankAccount?>(null) }
    var deleteTarget by remember { mutableStateOf<BankAccount?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddAccount = true }) {
                Icon(Icons.Default.Add, "افزودن حساب")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("حساب‌های بانکی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (accounts.isEmpty()) {
                item { EmptyState("هنوز حساب بانکی تعریف نشده.\nبا دکمه + حساب جدید بسازید.") }
            } else {
                items(accounts, key = { it.id }) { acc ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ItemIconBadge(acc.icon)
                                Column(Modifier.weight(1f)) {
                                    Text(acc.name, fontWeight = FontWeight.Bold)
                                    if (acc.bankName.isNotBlank()) Text(acc.bankName, style = MaterialTheme.typography.bodySmall)
                                    if (acc.accountNumber.isNotBlank()) Text(acc.accountNumber, style = MaterialTheme.typography.labelSmall)
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                FilledTonalButton(onClick = { showDeposit = acc }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("واریز")
                                }
                                OutlinedButton(onClick = { showWithdraw = acc }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Remove, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("برداشت")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { selectedAccountForReport = acc }, modifier = Modifier.weight(1f)) {
                                    Text("گزارش")
                                }
                                OutlinedButton(
                                    onClick = { deleteTarget = acc },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("حذف")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("آخرین تراکنش‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (transactions.isEmpty()) {
                item { Text("تراکنشی ثبت نشده", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(transactions.take(30), key = { it.id }) { tx ->
                    val accName = accounts.find { it.id == tx.accountId }?.name ?: "حساب"
                    ListItem(
                        headlineContent = { Text(tx.description.ifBlank { tx.type }) },
                        supportingContent = { Text("$accName • ${CurrencyUtil.format(tx.amount)}") },
                        leadingContent = {
                            Icon(
                                if (tx.type == "DEPOSIT") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                null,
                                tint = if (tx.type == "DEPOSIT") Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    )
                }
            }
        }
    }

    deleteTarget?.let { acc ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف حساب") },
            text = { Text("آیا از حذف «${acc.name}» و تمام تراکنش‌های آن مطمئن هستید؟") },
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
        TxDialog("واریز به ${acc.name}", "DEPOSIT", onDismiss = { showDeposit = null }) { amount, date, desc ->
            viewModel.deposit(acc.id, amount, date, desc) { showDeposit = null }
        }
    }
    showWithdraw?.let { acc ->
        TxDialog("برداشت از ${acc.name}", "WITHDRAW", onDismiss = { showWithdraw = null }) { amount, date, desc ->
            viewModel.withdraw(acc.id, amount, date, desc) { showWithdraw = null }
        }
    }
    selectedAccountForReport?.let { acc ->
        val txs = transactions.filter { it.accountId == acc.id }
        AlertDialog(
            onDismissRequest = { selectedAccountForReport = null },
            title = { Text("گزارش ${acc.name}") },
            text = {
                Column {
                    Text("موجودی فعلی: ${CurrencyUtil.format(acc.balance)}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (txs.isEmpty()) Text("تراکنشی وجود ندارد")
                    else txs.take(20).forEach { tx ->
                        Text("${tx.type}: ${CurrencyUtil.format(tx.amount)} — ${tx.description}")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedAccountForReport = null }) { Text("بستن") } }
        )
    }
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
                onSave(BankAccount(
                    name = name.trim(),
                    bankName = bankName.trim(),
                    accountNumber = accountNumber.trim(),
                    balance = bal,
                    isDefault = isDefault
                ))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun TxDialog(title: String, type: String, onDismiss: () -> Unit, onConfirm: (Long, Long, String) -> Unit) {
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
