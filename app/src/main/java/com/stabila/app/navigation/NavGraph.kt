package com.stabila.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.stabila.feature.camera.ui.CameraScreen
import com.stabila.feature.dailytest.ui.DailyTestScreen
import com.stabila.feature.history.ui.HistoryScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stabila.core.ui.Indigo500
import com.stabila.core.ui.Zinc950



// ─── Nav Host ─────────────────────────────────────────────────────────────────
@Composable
fun StabilaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Observe the latest tremor score to drive the Adaptive Tremor Interface
    val homeViewModel: com.stabila.app.ui.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val latestScore by homeViewModel.latestScore.collectAsState()

    com.stabila.core.ui.TremorAdaptiveTheme(score = latestScore) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier
        ) {
            composable(Screen.Home.route) {
                com.stabila.app.ui.HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToDailyTest = {
                        navController.navigate(Screen.DailyTest.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCamera = {
                        navController.navigate(Screen.Camera.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.DailyTest.route) {
                DailyTestScreen()
            }
            composable(Screen.Camera.route) {
                CameraScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Settings.route) {
                com.stabila.app.ui.SettingsScreen()
            }
        }
    }
}


// ─── Placeholder Screen (replaced screen-by-screen in later phases) ───────────
@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFFAFAFA)
        )
    }
}
