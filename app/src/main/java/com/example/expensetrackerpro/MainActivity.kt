package com.example.expensetrackerpro

import AppNavHost
import android.app.UiModeManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.presentation.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash only while startup work is still running
        splashScreen.setKeepOnScreenCondition { !viewModel.uiState.value.isReady }
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.view.animate()
                .alpha(0f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(250L)
                .withEndAction { splashScreenViewProvider.remove() }
                .start()
        }

        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val darkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(uiState.themeMode, uiState.isThemeLoaded) {
                if (uiState.isThemeLoaded) applyNightMode(uiState.themeMode)
            }

            ExpenseTrackerProTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // The system bar insets are applied here once; consuming them stops the
                    // Scaffolds and bars nested inside from adding them again
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        AppNavHost(navController = navController)
                    }
                }
            }
        }
    }

    // Android 12+ remembers this per app, so the next launch's system splash matches the theme
    private fun applyNightMode(themeMode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val nightMode = when (themeMode) {
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        getSystemService(UiModeManager::class.java).setApplicationNightMode(nightMode)
    }
}
