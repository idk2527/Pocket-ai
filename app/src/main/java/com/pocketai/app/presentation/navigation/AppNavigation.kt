package com.pocketai.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketai.app.presentation.add.AddExpenseScreen
import com.pocketai.app.presentation.analytics.AnalyticsScreen
import com.pocketai.app.presentation.home.HomeScreen
import com.pocketai.app.presentation.detail.ExpenseDetailScreen
import com.pocketai.app.presentation.receipts.ReceiptsScreen
import com.pocketai.app.presentation.download.ModelDownloadScreen
import com.pocketai.app.presentation.settings.SettingsScreen
import com.pocketai.app.services.ModelDownloadManager

@Composable
fun AppNavigation(
    mainViewModel: com.pocketai.app.viewmodel.MainViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    downloadManager: ModelDownloadManager
) {
    val navController = androidx.navigation.compose.rememberNavController()
    val onboardingState by mainViewModel.isOnboardingCompleted.collectAsState()
    
    // Show splash/loading while reading prefs
    if (onboardingState == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    val startDestination = if (onboardingState == true) {
        if (downloadManager.areModelsDownloaded()) {
            Screen.Dashboard.route
        } else {
            Screen.ModelDownload.route
        }
    } else {
        "onboarding"
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = { 
            if (currentRoute != "onboarding" && currentRoute != Screen.ModelDownload.route) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = navController, 
                startDestination = startDestination,
                enterTransition = { 
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) + 
                    androidx.compose.animation.slideInHorizontally(initialOffsetX = { 1000 }) 
                },
                exitTransition = { 
                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400)) + 
                    androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -1000 }) 
                },
                popEnterTransition = { 
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) + 
                    androidx.compose.animation.slideInHorizontally(initialOffsetX = { -1000 }) 
                },
                popExitTransition = { 
                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400)) + 
                    androidx.compose.animation.slideOutHorizontally(targetOffsetX = { 1000 }) 
                }
            ) {
                composable("onboarding") {
                    com.pocketai.app.presentation.onboarding.OnboardingScreen(navController = navController)
                }
                composable(Screen.ModelDownload.route) {
                    ModelDownloadScreen(
                        navController = navController,
                        downloadManager = downloadManager
                    )
                }
                
                composable(Screen.Dashboard.route) {
                    HomeScreen(navController = navController)
                }
                composable(Screen.Receipts.route) {
                    ReceiptsScreen(navController = navController)
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen(navController = navController)
                }
                composable(
                    route = Screen.AddExpense.routeWithArg + "?uri={uri}",
                    arguments = listOf(
                        navArgument(Screen.AddExpense.EXPENSE_ID_ARG) { 
                            type = NavType.IntType
                            defaultValue = -1
                        },
                        navArgument("uri") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val expenseId = backStackEntry.arguments?.getInt(Screen.AddExpense.EXPENSE_ID_ARG)
                    val passedUri = backStackEntry.arguments?.getString("uri")
                    AddExpenseScreen(
                        navController = navController, 
                        expenseId = if (expenseId == -1) null else expenseId,
                        passedUri = passedUri
                    )
                }
                composable(
                    route = Screen.ExpenseDetail.routeWithArg,
                    arguments = listOf(navArgument(Screen.ExpenseDetail.EXPENSE_ID_ARG) { type = NavType.IntType })
                ) { backStackEntry ->
                    val expenseId = backStackEntry.arguments?.getInt(Screen.ExpenseDetail.EXPENSE_ID_ARG)
                    if (expenseId != null) {
                        ExpenseDetailScreen(navController = navController, expenseId = expenseId)
                    }
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(navController = navController)
                }
                composable(
                    route = Screen.DigitalReceipt.routeWithArg,
                    arguments = listOf(navArgument(Screen.DigitalReceipt.EXPENSE_ID_ARG) { type = NavType.IntType })
                ) { backStackEntry ->
                    val expenseId = backStackEntry.arguments?.getInt(Screen.DigitalReceipt.EXPENSE_ID_ARG)
                    if (expenseId != null) {
                        com.pocketai.app.presentation.receipt.DigitalReceiptScreen(navController = navController, expenseId = expenseId)
                    }
                }
            }
        }
    }
}