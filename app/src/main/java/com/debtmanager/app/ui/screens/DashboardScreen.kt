package com.debtmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.ui.components.*
import com.debtmanager.app.ui.theme.StatusOverdue
import com.debtmanager.app.ui.theme.StatusPaid
import com.debtmanager.app.ui.theme.StatusUpcoming
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.viewmodel.DashboardUiState
import com.debtmanager.app.viewmodel.MainViewModel
import androidx.navigation.NavController
import com.debtmanager.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController) {
    val state by viewModel.dashboardState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("داشبورد مالی") },
            actions = {
                IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                    Icon(Icons.Default.History, "تاریخچه")
                }
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Default.Settings, "تنظیمات")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("خلاصه ماه جاری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        "سررسید ماه", CurrencyUtil.formatWithoutUnit(state.monthDueTotal) + " ریال",
                        StatusUpcoming, Modifier.weight(1f)
                    )
                    StatCard(
                        "پرداخت‌شده", CurrencyUtil.formatWithoutUnit(state.monthPaidTotal) + " ریال",
                        StatusPaid, Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("پرداخت‌نشده", "${com.debtmanager.app.util.PersianDateUtil.toPersianDigits(state.unpaidCount)} مورد", StatusOverdue, Modifier.weight(1f))
                    StatCard("باقی‌مانده کل", CurrencyUtil.formatWithoutUnit(state.remaining) + " ریال", StatusOverdue, Modifier.weight(1f))
                }
            }

            item {
                Text("آمار کلی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("کل بدهی")
                            AmountText(state.totalDebt)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("کل پرداخت‌شده")
                            AmountText(state.totalPaid, style = MaterialTheme.typography.bodyLarge.copy(color = StatusPaid))
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مانده", fontWeight = FontWeight.Bold)
                            AmountText(state.remaining, style = MaterialTheme.typography.titleMedium.copy(color = StatusOverdue))
                        }
                    }
                }
            }

            if (state.overdue.isNotEmpty()) {
                item {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = StatusOverdue)
                        Spacer(Modifier.width(8.dp))
                        Text("معوق / عقب‌افتاده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusOverdue)
                    }
                }
                items(state.overdue) { item ->
                    UpcomingCard(item)
                }
            }

            item {
                Text("پرداخت‌های پیش‌رو (۳۰ روز)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (state.upcoming.isEmpty()) {
                item { Text("موردی یافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.upcoming) { item ->
                    UpcomingCard(item)
                }
            }
        }
    }
}

@Composable
fun UpcomingCard(item: com.debtmanager.app.data.repository.UpcomingItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Medium)
                DateText(item.dueDate, short = true)
                Text(item.type.label, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                AmountText(item.amount)
                StatusChip(item.status)
            }
        }
    }
}
