package com.debtmanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavController
import com.debtmanager.app.data.entity.Loan
import com.debtmanager.app.ui.components.*
import com.debtmanager.app.ui.navigation.Screen
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.util.PersianDateUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(viewModel: MainViewModel, navController: NavController) {
    val loans by viewModel.loans.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Loan?>(null) }
    var deleteTarget by remember { mutableStateOf<Loan?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.Add, "افزودن")
            }
        }
    ) { padding ->
        if (loans.isEmpty()) {
            EmptyState("وامی ثبت نشده است", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(loans, key = { it.id }) { loan ->
                    LoanCard(
                        loan = loan,
                        onClick = { navController.navigate(Screen.LoanDetail.createRoute(loan.id)) },
                        onEdit = { editTarget = loan },
                        onDelete = { deleteTarget = loan }
                    )
                }
            }
        }
    }

    if (showAdd) AddLoanDialog(viewModel, onDismiss = { showAdd = false }, onDone = { showAdd = false })
    editTarget?.let { loan ->
        EditLoanDialog(loan, onDismiss = { editTarget = null }) { updated ->
            viewModel.updateLoan(updated) { editTarget = null }
        }
    }
    deleteTarget?.let { loan ->
        ConfirmDialog("حذف وام", "آیا از حذف «${loan.title}» مطمئن هستید؟",
            onConfirm = { viewModel.deleteLoan(loan) { deleteTarget = null } },
            onDismiss = { deleteTarget = null })
    }
}

@Composable
fun LoanCard(loan: Loan, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val remaining = loan.totalAmount - (loan.paidCount * loan.installmentAmount)
    val progress = if (loan.installmentCount > 0) loan.paidCount.toFloat() / loan.installmentCount else 0f

    ElevatedCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemIconBadge(loan.icon.ifBlank { "account_balance" })
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Text(loan.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("مبلغ کل: ${CurrencyUtil.format(loan.totalAmount)}")
                Text("قسط: ${PersianDateUtil.toPersianDigits(loan.paidCount)} از ${PersianDateUtil.toPersianDigits(loan.installmentCount)}")
                Text("باقی‌مانده: ${CurrencyUtil.format(remaining)}")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            Column {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "ویرایش") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف") }
            }
        }
    }
}

@Composable
fun AddLoanDialog(viewModel: MainViewModel, onDismiss: () -> Unit, onDone: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var installmentCount by remember { mutableStateOf("") }
    var installmentAmount by remember { mutableStateOf("") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var paymentDay by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("account_balance") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن وام") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(title, { title = it }, label = { Text("عنوان *") }, modifier = Modifier.fillMaxWidth()) }
                item { AmountTextField(totalAmount, { totalAmount = it }, "مبلغ کل (ریال)") }
                item { AmountTextField(installmentCount, { installmentCount = it }, "تعداد اقساط") }
                item { AmountTextField(installmentAmount, { installmentAmount = it }, "مبلغ هر قسط (ریال)") }
                item { JalaliDatePickerField(startDate, { startDate = it }, "تاریخ شروع") }
                item { AmountTextField(paymentDay, { paymentDay = it }, "روز پرداخت ماهانه") }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth()) }
                item { IconPicker(selectedIcon = icon, onIconSelected = { icon = it }, label = "آیکون") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val total = CurrencyUtil.parse(totalAmount) ?: return@TextButton
                val count = installmentCount.toIntOrNull() ?: return@TextButton
                val amount = CurrencyUtil.parse(installmentAmount) ?: return@TextButton
                val day = paymentDay.toIntOrNull() ?: 1
                if (title.isBlank()) return@TextButton
                viewModel.addLoan(title, total, count, amount, startDate, day, notes, icon, onDone)
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun EditLoanDialog(loan: Loan, onDismiss: () -> Unit, onSave: (Loan) -> Unit) {
    var title by remember { mutableStateOf(loan.title) }
    var notes by remember { mutableStateOf(loan.notes) }
    var icon by remember { mutableStateOf(loan.icon.ifBlank { "account_balance" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش وام") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth())
                IconPicker(selectedIcon = icon, onIconSelected = { icon = it }, label = "آیکون")
                Text(
                    "برای تغییر مبلغ یا اقساط، وام را حذف و دوباره ثبت کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) return@TextButton
                onSave(loan.copy(title = title, notes = notes, icon = icon))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(viewModel: MainViewModel, loanId: Long, onBack: () -> Unit) {
    val loans by viewModel.loans.collectAsState()
    val loan = loans.find { it.id == loanId }
    val installments by viewModel.getInstallments(loanId).collectAsState(initial = emptyList())
    var payTarget by remember { mutableStateOf<com.debtmanager.app.data.entity.LoanInstallment?>(null) }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = loan?.title ?: "جزئیات وام",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            loan?.let { l ->
                item {
                    ElevatedCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ItemIconBadge(l.icon.ifBlank { "account_balance" })
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("مبلغ کل: ${CurrencyUtil.format(l.totalAmount)}")
                                Text("هر قسط: ${CurrencyUtil.format(l.installmentAmount)}")
                                val remaining = l.totalAmount - l.paidCount * l.installmentAmount
                                Text("باقی‌مانده: ${CurrencyUtil.format(remaining)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            items(installments, key = { it.id }) { inst ->
                val status = com.debtmanager.app.data.repository.getInstallmentStatus(inst.dueDate, inst.isPaid)
                ElevatedCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            DateText(inst.dueDate)
                            AmountText(inst.amount)
                            StatusChip(status)
                        }
                        if (!inst.isPaid) {
                            Button(onClick = { payTarget = inst }) { Text("پرداخت") }
                        } else {
                            inst.paidDate?.let { DateText(it, short = true) }
                        }
                    }
                }
            }
        }
    }

    payTarget?.let { inst ->
        PayInstallmentDialog(inst, onDismiss = { payTarget = null }) { amount, date ->
            viewModel.payInstallment(inst, amount, date) { payTarget = null }
        }
    }
}

@Composable
fun PayInstallmentDialog(
    installment: com.debtmanager.app.data.entity.LoanInstallment,
    onDismiss: () -> Unit,
    onPay: (Long, Long) -> Unit
) {
    var amount by remember { mutableStateOf(CurrencyUtil.formatWithoutUnit(installment.amount)) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت پرداخت قسط") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountTextField(amount, { amount = it }, "مبلغ پرداختی (ریال)")
                JalaliDatePickerField(date, { date = it }, "تاریخ پرداخت")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                onPay(a, date)
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
