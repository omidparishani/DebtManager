package com.debtmanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.BankAccount
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
    var detailAccount by remember { mutableStateOf<BankAccount?>(null) }
    var deleteTarget by remember { mutableStateOf<BankAccount?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddAccount = true }) {
                Icon(Icons.Default.Add, "افزودن حساب")
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
                contentPadding = PaddingValues(16.dp),
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

    // جزئیات حساب + تراکنش‌ها
    detailAccount?.let { acc ->
        val txs = transactions.filter { it.accountId == acc.id }.sortedByDescending { it.date }
        AlertDialog(
            onDismissRequest = { detailAccount = null },
            title = { Text(acc.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("موجودی: ${CurrencyUtil.format(acc.balance)}", fontWeight = FontWeight.Bold)
                    if (acc.bankName.isNotBlank()) Text("بانک: ${acc.bankName}")
                    HorizontalDivider()
                    Text("تراکنش‌ها", fontWeight = FontWeight.Medium)
                    if (txs.isEmpty()) {
                        Text("تراکنشی ثبت نشده")
                    } else {
                        txs.take(25).forEach { tx ->
                            val sign = if (tx.type == "DEPOSIT") "+" else "−"
                            val color = if (tx.type == "DEPOSIT") Color(0xFF2E7D32) else Color(0xFFC62828)
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(tx.description.ifBlank { tx.type })
                                    Text(
                                        PersianDateUtil.formatShort(tx.date),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    "$sign ${CurrencyUtil.format(tx.amount)}",
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
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
