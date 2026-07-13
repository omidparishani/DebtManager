package com.debtmanager.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debtmanager.app.R
import com.debtmanager.app.ui.components.*
import com.debtmanager.app.ui.navigation.Screen
import com.debtmanager.app.ui.theme.StatusOverdue
import com.debtmanager.app.ui.theme.StatusPaid
import com.debtmanager.app.ui.theme.StatusUpcoming
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.util.ItemIcons
import com.debtmanager.app.util.PersianDateUtil
import com.debtmanager.app.viewmodel.MainViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController) {
    val state by viewModel.dashboardState.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userIcon by viewModel.userIcon.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("داشبورد مالی") },
            actions = {
                IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                    if (userIcon == "avatar_brand") {
                        Image(
                            painter = painterResource(R.drawable.brand_logo),
                            contentDescription = "پروفایل",
                            modifier = Modifier.size(32.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(ItemIcons.resolve(userIcon), "پروفایل")
                    }
                }
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
                ElevatedCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (userIcon == "avatar_brand") {
                            Image(
                                painter = painterResource(R.drawable.brand_logo),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            ItemIconBadge(userIcon, size = 48)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "سلام، $userName!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "وضعیت مالی شما در یک نگاه",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text("خلاصه ماه جاری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        "سررسید ماه", CurrencyUtil.formatWithoutUnit(state.monthDueTotal) + " ریال",
                        StatusUpcoming, Modifier.weight(1f), icon = Icons.Default.Event
                    )
                    StatCard(
                        "پرداخت‌شده", CurrencyUtil.formatWithoutUnit(state.monthPaidTotal) + " ریال",
                        StatusPaid, Modifier.weight(1f), icon = Icons.Default.CheckCircle
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        "پرداخت‌نشده", "${PersianDateUtil.toPersianDigits(state.unpaidCount)} مورد",
                        StatusOverdue, Modifier.weight(1f), icon = Icons.Default.PendingActions
                    )
                    StatCard(
                        "باقی‌مانده کل", CurrencyUtil.formatWithoutUnit(state.remaining) + " ریال",
                        StatusOverdue, Modifier.weight(1f), icon = Icons.Default.AccountBalanceWallet
                    )
                }
            }

            item {
                Text("آمار کلی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ElevatedCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary)
                        Text("خلاصه کل بدهی‌ها", fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, null, tint = StatusOverdue, modifier = Modifier.size(18.dp))
                            Text("کل بدهی")
                        }
                        AmountText(state.totalDebt)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, tint = StatusPaid, modifier = Modifier.size(18.dp))
                            Text("کل پرداخت‌شده")
                        }
                        AmountText(state.totalPaid, style = MaterialTheme.typography.bodyLarge.copy(color = StatusPaid))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Savings, null, tint = StatusOverdue, modifier = Modifier.size(18.dp))
                            Text("مانده", fontWeight = FontWeight.Bold)
                        }
                        AmountText(state.remaining, style = MaterialTheme.typography.titleMedium.copy(color = StatusOverdue))
                    }
                }
            }

            if (state.overdue.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("پرداخت‌های پیش‌رو (۳۰ روز)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
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
    val iconKey = item.icon.ifBlank { ItemIcons.defaultForPaymentType(item.type.name) }
    val iconTint = when (item.status) {
        com.debtmanager.app.data.entity.InstallmentStatus.PAID -> StatusPaid
        com.debtmanager.app.data.entity.InstallmentStatus.OVERDUE -> StatusOverdue
        else -> StatusUpcoming
    }

    ElevatedCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemIconBadge(iconKey, tint = iconTint)
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Medium)
                DateText(item.dueDate, short = true)
                Text(item.type.label, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                AmountText(item.amount)
                StatusChip(item.status)
            }
        }
    }
}
