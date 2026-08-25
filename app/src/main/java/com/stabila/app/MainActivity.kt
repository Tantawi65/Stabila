package com.stabila.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.stabila.app.navigation.StabilaNavHost
import androidx.compose.material3.MaterialTheme
import com.stabila.core.ui.StabilaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var userPrefs: com.stabila.core.data.UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePref by userPrefs.themePreference.collectAsState(initial = "SYSTEM")
            val darkTheme = when (themePref) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            StabilaTheme(darkTheme = darkTheme) {
                StabilaApp()
            }
        }
    }
}

@Composable
fun StabilaApp() {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        StabilaNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
