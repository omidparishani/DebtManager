package com.debtmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debtmanager.app.data.entity.PaymentHistory
import com.debtmanager.app.data.entity.PaymentType
import com.debtmanager.app.ui.components.AmountText
import com.debtmanager.app.ui.components.DateText
import com.debtmanager.app.ui.components.EmptyState
import com.debtmanager.app.ui.components.SecondaryTopBar
import androidx.navigation.NavController
import com.debtmanager.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel, navController: NavController) {
    val history by viewModel.paymentHistory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<String?>(null) }
    var sortAscending by remember { mutableStateOf(false) }

    val filtered = remember(history, searchQuery, filterType, sortAscending) {
        var list = history
        if (filterType != null) list = list.filter { it.type == filterType }
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.description.contains(searchQuery, ignoreCase = true) ||
                    PaymentType.entries.find { t -> t.name == it.type }?.label?.contains(searchQuery) == true
            }
        }
        if (sortAscending) list.sortedBy { it.date } else list.sortedByDescending { it.date }
    }

    Column(Modifier.fillMaxSize()) {
        SecondaryTopBar(
            title = "تاریخچه پرداخت‌ها",
            onBack = { navController.popBackStack() }
        )
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("جستجو") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filterType == null, onClick = { filterType = null }, label = { Text("همه") })
                PaymentType.entries.forEach { type ->
                    FilterChip(
                        selected = filterType == type.name,
                        onClick = { filterType = if (filterType == type.name) null else type.name },
                        label = { Text(type.label) }
                    )
                }
            }
            TextButton(onClick = { sortAscending = !sortAscending }) {
                Text(if (sortAscending) "مرتب‌سازی: قدیمی‌ترین" else "مرتب‌سازی: جدیدترین")
            }
        }
        if (filtered.isEmpty()) {
            EmptyState("پرداختی ثبت نشده است")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { item ->
                    HistoryCard(item)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: PaymentHistory) {
    val typeLabel = PaymentType.entries.find { it.name == item.type }?.label ?: item.type
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(item.description.ifBlank { typeLabel })
                Text(typeLabel, style = MaterialTheme.typography.labelSmall)
                DateText(item.date)
            }
            AmountText(item.amount)
        }
    }
}
