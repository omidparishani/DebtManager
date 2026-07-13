package com.debtmanager.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "داشبورد", Icons.Filled.Dashboard)
    data object Loans : Screen("loans", "وام‌ها", Icons.Filled.AccountBalance)
    data object Checks : Screen("checks", "چک‌ها", Icons.Filled.Receipt)
    data object Debts : Screen("debts", "بدهکاری‌ها", Icons.Filled.People)
    data object Recurring : Screen("recurring", "اقساط دوره‌ای", Icons.Filled.Autorenew)
    data object History : Screen("history", "تاریخچه", Icons.Filled.History)
    data object Settings : Screen("settings", "تنظیمات", Icons.Filled.Settings)
    data object Profile : Screen("profile", "پروفایل", Icons.Filled.AccountCircle)

    data object LoanDetail : Screen("loan/{loanId}", "جزئیات وام", Icons.Filled.AccountBalance) {
        fun createRoute(loanId: Long) = "loan/$loanId"
    }
    data object AddLoan : Screen("add_loan", "افزودن وام", Icons.Filled.Add)
    data object AddCheck : Screen("add_check", "افزودن چک", Icons.Filled.Add)
    data object EditCheck : Screen("edit_check/{checkId}", "ویرایش چک", Icons.Filled.Edit) {
        fun createRoute(checkId: Long) = "edit_check/$checkId"
    }
    data object AddDebt : Screen("add_debt", "افزودن بدهی", Icons.Filled.Add)
    data object EditDebt : Screen("edit_debt/{debtId}", "ویرایش بدهی", Icons.Filled.Edit) {
        fun createRoute(debtId: Long) = "edit_debt/$debtId"
    }
    data object AddRecurring : Screen("add_recurring", "افزودن قسط دوره‌ای", Icons.Filled.Add)
    data object Lock : Screen("lock", "قفل اپ", Icons.Filled.Lock)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Loans,
    Screen.Checks,
    Screen.Debts,
    Screen.Recurring
)
