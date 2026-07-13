package com.debtmanager.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "داشبورد", Icons.Filled.SpaceDashboard)
    data object Loans : Screen("loans", "وام‌ها", Icons.Filled.AccountBalance)
    data object Checks : Screen("checks", "چک‌ها", Icons.Filled.Payments)
    data object Debts : Screen("debts", "بدهکاری‌ها", Icons.Filled.Groups)
    data object Recurring : Screen("recurring", "اقساط دوره‌ای", Icons.Filled.EventRepeat)
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

data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, Icons.Filled.SpaceDashboard, Icons.Outlined.SpaceDashboard, "داشبورد"),
    BottomNavItem(Screen.Loans, Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance, "وام‌ها"),
    BottomNavItem(Screen.Checks, Icons.Filled.Payments, Icons.Outlined.Payments, "چک‌ها"),
    BottomNavItem(Screen.Debts, Icons.Filled.Groups, Icons.Outlined.Groups, "بدهکاری"),
    BottomNavItem(Screen.Recurring, Icons.Filled.EventRepeat, Icons.Outlined.EventRepeat, "دوره‌ای")
)

val bottomNavRoutes = bottomNavItems.map { it.screen.route }

fun bottomNavItemForRoute(route: String?) = bottomNavItems.find { it.screen.route == route }
