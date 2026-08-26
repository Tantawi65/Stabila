package com.stabila.app.navigation

/**
 * Sealed class of all top-level navigation destinations.
 *
 * Adding a new screen: add a new object here and a composable in NavGraph.kt.
 * No magic strings anywhere else in the app.
 */
sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object DailyTest  : Screen("daily_test")
    object Camera     : Screen("camera")
    object History    : Screen("history")
    object Settings   : Screen("settings")
    object KeyboardSetup : Screen("keyboard_setup")
}
