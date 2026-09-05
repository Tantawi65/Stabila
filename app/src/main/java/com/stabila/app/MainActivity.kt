package com.stabila.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.rememberNavController
import com.stabila.app.navigation.StabilaNavHost
import com.stabila.core.ui.StabilaTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var userPrefs: com.stabila.core.data.UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePref by userPrefs.themePreference.collectAsState(initial = "SYSTEM")
            val languagePref by userPrefs.languagePreference.collectAsState(initial = "SYSTEM")

            val darkTheme = when (themePref) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            val targetLocale = when (languagePref) {
                "ar" -> Locale("ar")
                "en" -> Locale("en")
                else -> Locale.getDefault()
            }

            val layoutDirection = if (targetLocale.language == "ar") {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            val baseContext = LocalContext.current
            val localizedConfig = remember(targetLocale, baseContext) {
                Configuration(baseContext.resources.configuration).apply {
                    setLocale(targetLocale)
                    setLayoutDirection(targetLocale)
                }
            }

            val localizedContext = remember(targetLocale, baseContext, localizedConfig) {
                object : ContextWrapper(baseContext) {
                    private val configContext = baseContext.createConfigurationContext(localizedConfig)
                    override fun getResources(): Resources {
                        return configContext.resources
                    }
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig,
                LocalLayoutDirection provides layoutDirection
            ) {
                StabilaTheme(darkTheme = darkTheme) {
                    StabilaApp()
                }
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            com.stabila.app.ui.components.StabilaBottomBar(navController = navController)
        }
    ) { innerPadding ->
        StabilaNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

