package com.debtmanager.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.debtmanager.app.ui.components.EnhancedBottomNav
import com.debtmanager.app.ui.components.MainTopBar
import com.debtmanager.app.ui.navigation.Screen
import com.debtmanager.app.ui.navigation.bottomNavItemForRoute
import com.debtmanager.app.ui.navigation.bottomNavRoutes
import com.debtmanager.app.ui.screens.*
import com.debtmanager.app.ui.theme.DebtManagerTheme
import com.debtmanager.app.viewmodel.MainViewModel
import java.util.Locale

class MainActivity : FragmentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Locale.setDefault(Locale("fa"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val darkMode by vm.darkMode.collectAsState()
            val pinEnabled by vm.pinEnabled.collectAsState()
            var unlocked by remember { mutableStateOf(!pinEnabled) }

            LaunchedEffect(pinEnabled) {
                if (!pinEnabled) unlocked = true
                else unlocked = false
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DebtManagerTheme(darkTheme = darkMode) {
                    if (pinEnabled && !unlocked) {
                        LockScreen(vm) { unlocked = true }
                    } else {
                        DebtManagerNavHost(vm)
                    }
                }
            }
        }
    }
}

@Composable
fun DebtManagerNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes
    val currentNavItem = bottomNavItemForRoute(currentRoute)

    Scaffold(
        topBar = {
            if (currentNavItem != null) {
                MainTopBar(
                    navItem = currentNavItem,
                    viewModel = viewModel,
                    navController = navController
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                EnhancedBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(viewModel, navController) }
            composable(Screen.Loans.route) { LoansScreen(viewModel, navController) }
            composable(Screen.Checks.route) { ChecksScreen(viewModel) }
            composable(Screen.Debts.route) { DebtsScreen(viewModel) }
            composable(Screen.Recurring.route) { RecurringScreen(viewModel) }
            composable(Screen.History.route) { HistoryScreen(viewModel, navController) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel, navController) }
            composable(Screen.Profile.route) { ProfileScreen(viewModel, navController) }
            composable(
                route = Screen.LoanDetail.route,
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { entry ->
                val loanId = entry.arguments?.getLong("loanId") ?: 0L
                LoanDetailScreen(viewModel, loanId) { navController.popBackStack() }
            }
        }
    }
}
