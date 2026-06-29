package com.example.budgetbuddy.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.budgetbuddy.ui.auth.ForgotPasswordScreen
import com.example.budgetbuddy.ui.auth.LoginScreen
import com.example.budgetbuddy.ui.auth.RegisterScreen
import com.example.budgetbuddy.ui.dashboard.DashboardScreen
import com.example.budgetbuddy.ui.transaction.TransactionScreen
import com.example.budgetbuddy.ui.profile.ProfileScreen
import com.example.budgetbuddy.ui.analytics.AnalyticsScreen
import com.example.budgetbuddy.ui.budget.BudgetScreen
import com.example.budgetbuddy.ui.settings.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val ANALYTICS = "analytics"
    const val PROFILE = "profile"
    const val BUDGET = "budget"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Routes.DASHBOARD,
        Routes.TRANSACTIONS,
        Routes.ANALYTICS,
        Routes.PROFILE,
        Routes.BUDGET
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                    onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen()
            }

            composable(Routes.TRANSACTIONS) {
                TransactionScreen()
            }

            composable(Routes.ANALYTICS) {
                AnalyticsScreen()
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Routes.SETTINGS)
                    }
                )
            }

            composable(Routes.BUDGET) {
                BudgetScreen()
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}