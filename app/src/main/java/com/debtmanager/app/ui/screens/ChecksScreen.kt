package com.debtmanager.app.ui.screens

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
import com.debtmanager.app.data.entity.CheckEntity
import com.debtmanager.app.data.entity.CheckStatus
import com.debtmanager.app.ui.components.*
import com.debtmanager.app.ui.theme.checkStatusColor
import com.debtmanager.app.ui.theme.checkStatusLabel
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecksScreen(viewModel: MainViewModel) {
    val checks by viewModel.checks.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<CheckEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CheckEntity?>(null) }
    var collectTarget by remember { mutableStateOf<CheckEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("چک‌های صادرشده") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "افزودن") }
        }
    ) { padding ->
        if (checks.isEmpty()) {
            EmptyState("چکی ثبت نشده است", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(checks, key = { it.id }) { check ->
                    CheckCard(
                        check = check,
                        onEdit = { editTarget = check },
                        onDelete = { deleteTarget = check },
                        onCollect = { if (check.status == CheckStatus.PENDING.name) collectTarget = check }
                    )
                }
            }
        }
    }

    if (showAdd) CheckFormDialog(null, onDismiss = { showAdd = false }) { check ->
        viewModel.addCheck(check) { showAdd = false }
    }
    editTarget?.let { check ->
        CheckFormDialog(check, onDismiss = { editTarget = null }) { updated ->
            viewModel.updateCheck(updated) { editTarget = null }
        }
    }
    deleteTarget?.let { check ->
        ConfirmDialog("حذف چک", "آیا مطمئن هستید؟",
            onConfirm = { viewModel.deleteCheck(check) { deleteTarget = null } },
            onDismiss = { deleteTarget = null })
    }
    collectTarget?.let { check ->
        var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
        AlertDialog(
            onDismissRequest = { collectTarget = null },
            title = { Text("ثبت وصول چک") },
            text = { JalaliDatePickerField(date, { date = it }, "تاریخ وصول") },
            confirmButton = {
                TextButton(onClick = { viewModel.collectCheck(check, date) { collectTarget = null } }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { collectTarget = null }) { Text("انصراف") } }
        )
    }
}

@Composable
fun CheckCard(check: CheckEntity, onEdit: () -> Unit, onDelete: () -> Unit, onCollect: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(check.payee, fontWeight = FontWeight.Bold)
                StatusChip(checkStatusLabel(check.status), checkStatusColor(check.status))
            }
            AmountText(check.amount)
            DateText(check.dueDate)
            if (check.bankName.isNotBlank()) Text("بانک: ${check.bankName}")
            if (check.checkNumber.isNotBlank()) Text("شماره: ${check.checkNumber}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (check.status == CheckStatus.PENDING.name) {
                    TextButton(onClick = onCollect) { Text("وصول") }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "ویرایش") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckFormDialog(existing: CheckEntity?, onDismiss: () -> Unit, onSave: (CheckEntity) -> Unit) {
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var dueDate by remember { mutableLongStateOf(existing?.dueDate ?: System.currentTimeMillis()) }
    var bankName by remember { mutableStateOf(existing?.bankName ?: "") }
    var checkNumber by remember { mutableStateOf(existing?.checkNumber ?: "") }
    var payee by remember { mutableStateOf(existing?.payee ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var extraInfo by remember { mutableStateOf(existing?.extraInfo ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: CheckStatus.PENDING.name) }
    var statusExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "افزودن چک" else "ویرایش چک") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { AmountTextField(amount, { amount = it }, "مبلغ (ریال) *") }
                item { JalaliDatePickerField(dueDate, { dueDate = it }, "تاریخ سررسید *") }
                item { OutlinedTextField(bankName, { bankName = it }, label = { Text("نام بانک") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(checkNumber, { checkNumber = it }, label = { Text("شماره چک") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(payee, { payee = it }, label = { Text("گیرنده *") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(description, { description = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(extraInfo, { extraInfo = it }, label = { Text("اطلاعات تکمیلی") }, modifier = Modifier.fillMaxWidth()) }
                if (existing != null) {
                    item {
                        ExposedDropdownMenuBox(statusExpanded, { statusExpanded = it }) {
                            OutlinedTextField(
                                checkStatusLabel(status), {}, readOnly = true,
                                label = { Text("وضعیت") },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(statusExpanded, { statusExpanded = false }) {
                                CheckStatus.entries.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(checkStatusLabel(s.name)) },
                                        onClick = { status = s.name; statusExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = CurrencyUtil.parse(amount) ?: return@TextButton
                if (payee.isBlank()) return@TextButton
                onSave(CheckEntity(
                    id = existing?.id ?: 0,
                    amount = a, dueDate = dueDate, bankName = bankName,
                    checkNumber = checkNumber, payee = payee,
                    description = description, extraInfo = extraInfo, status = status
                ))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
