package com.debtmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.debtmanager.app.data.entity.Debt
import com.debtmanager.app.ui.components.AmountText
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
    val contact = contacts.find { it.id == contactId }
    val related = remember(debts, contactId, contact) {
        debts.filter { d ->
            d.contactId == contactId || (contact != null && d.creditorName == contact.name)
        }
    }
    val totalDebt = related.filter { !it.isCredit }.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0) }
    val totalCredit = related.filter { it.isCredit }.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0) }
    val net = totalCredit - totalDebt // positive = they owe you

    var showAdd by remember { mutableStateOf(false) }
    var addAsCredit by remember { mutableStateOf(false) }

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
            }
            item {
                Text("ریز تراکنش‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (related.isEmpty()) {
                item { Text("هنوز موردی ثبت نشده", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(related, key = { it.id }) { debt ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
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
                                Text("مانده: ${CurrencyUtil.format(debt.totalAmount - debt.paidAmount)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd && contact != null) {
        val contactsList by viewModel.contacts.collectAsState(initial = emptyList())
        DebtFormDialog(
            existing = Debt(
                creditorName = contact.name,
                category = "PERSON",
                totalAmount = 0,
                date = System.currentTimeMillis(),
                isCredit = addAsCredit,
                contactId = contact.id
            ),
            contacts = contactsList,
            onDismiss = { showAdd = false },
            onSave = { debt ->
                viewModel.addDebt(
                    debt.copy(
                        creditorName = contact.name,
                        contactId = contact.id,
                        isCredit = addAsCredit,
                        totalAmount = debt.totalAmount
                    )
                ) { showAdd = false }
            },
            onAddContact = { c, done -> viewModel.addContact(c, done) }
        )
    }
}
