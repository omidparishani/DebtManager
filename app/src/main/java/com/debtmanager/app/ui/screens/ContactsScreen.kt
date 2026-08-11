package com.debtmanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.debtmanager.app.data.entity.Contact
import com.debtmanager.app.data.entity.ContactType
import com.debtmanager.app.ui.components.EmptyState
import com.debtmanager.app.ui.components.ItemIconBadge
import com.debtmanager.app.ui.navigation.Screen
import com.debtmanager.app.ui.theme.StatusOverdue
import com.debtmanager.app.ui.theme.StatusPaid
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(viewModel: MainViewModel, navController: NavController? = null) {
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    val debts by viewModel.debts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Contact?>(null) }
    var deleteTarget by remember { mutableStateOf<Contact?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "افزودن شخص")
            }
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            EmptyState("هنوز شخصی تعریف نشده است.\nبا دکمه + شخص جدید اضافه کنید.", Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    val related = debts.filter {
                        it.contactId == contact.id || it.creditorName == contact.name
                    }
                    val debtSum = related.filter { !it.isCredit }.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0) }
                    val creditSum = related.filter { it.isCredit }.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0) }
                    val net = creditSum - debtSum

                    ElevatedCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController?.navigate(Screen.ContactDetail.createRoute(contact.id))
                            },
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.elevatedCardElevation(3.dp)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ItemIconBadge(contact.icon.ifBlank { "person" })
                            Column(Modifier.weight(1f)) {
                                Text(contact.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                val typeLabel = ContactType.entries.find { it.name == contact.type }?.label ?: contact.type
                                Text(typeLabel, style = MaterialTheme.typography.labelSmall)
                                if (contact.phone.isNotBlank()) {
                                    Text(contact.phone, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    when {
                                        net > 0 -> "طلب شما: ${CurrencyUtil.format(net)}"
                                        net < 0 -> "بدهی شما: ${CurrencyUtil.format(-net)}"
                                        related.isEmpty() -> "بدون تراکنش"
                                        else -> "تسویه"
                                    },
                                    color = when {
                                        net > 0 -> StatusPaid
                                        net < 0 -> StatusOverdue
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column {
                                IconButton(onClick = { editTarget = contact }) { Icon(Icons.Default.Edit, null) }
                                IconButton(onClick = { deleteTarget = contact }) { Icon(Icons.Default.Delete, null) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ContactFormDialog(null, onDismiss = { showAdd = false }) { c ->
            viewModel.addContact(c) { showAdd = false }
        }
    }
    editTarget?.let { c ->
        ContactFormDialog(c, onDismiss = { editTarget = null }) { updated ->
            viewModel.updateContact(updated) { editTarget = null }
        }
    }
    deleteTarget?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف شخص") },
            text = { Text("آیا از حذف «${c.name}» مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteContact(c) { deleteTarget = null } }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("انصراف") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFormDialog(existing: Contact?, onDismiss: () -> Unit, onSave: (Contact) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: ContactType.PERSON.name) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "افزودن شخص / فروشگاه" else "ویرایش") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("تلفن") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded, { expanded = it }) {
                    val label = ContactType.entries.find { it.name == type }?.label ?: ""
                    OutlinedTextField(label, {}, readOnly = true, label = { Text("نوع") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        ContactType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { type = t.name; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) return@TextButton
                onSave(Contact(
                    id = existing?.id ?: 0,
                    name = name.trim(),
                    phone = phone.trim(),
                    notes = notes.trim(),
                    type = type
                ))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
