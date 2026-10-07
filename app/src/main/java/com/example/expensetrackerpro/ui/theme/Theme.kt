package com.example.expensetrackerpro

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.expensetrackerpro.ui.theme.CharcoalBackgroundDark
import com.example.expensetrackerpro.ui.theme.CharcoalSurfaceDark
import com.example.expensetrackerpro.ui.theme.CharcoalSurfaceVariantDark
import com.example.expensetrackerpro.ui.theme.FinancialRedError
import com.example.expensetrackerpro.ui.theme.SlateSecondary
import com.example.expensetrackerpro.ui.theme.VeridianGreenPrimary
import com.example.expensetrackerpro.ui.theme.VeridianGreenPrimaryDark
import com.example.expensetrackerpro.ui.theme.VeridianOnPrimary
import com.example.expensetrackerpro.ui.theme.VeridianOnPrimaryContainer
import com.example.expensetrackerpro.ui.theme.VeridianOnPrimaryDark
import com.example.expensetrackerpro.ui.theme.VeridianPrimaryContainer
import com.example.expensetrackerpro.ui.theme.VeridianPrimaryContainerDark
import com.example.expensetrackerpro.ui.theme.Typography

private val DarkColorScheme = darkColorScheme(
    primary = VeridianGreenPrimaryDark,
    onPrimary = VeridianOnPrimaryDark,
    primaryContainer = VeridianPrimaryContainerDark,
    background = CharcoalBackgroundDark,
    surface = CharcoalSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = CharcoalSurfaceVariantDark,
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = VeridianGreenPrimary,
    onPrimary = VeridianOnPrimary,
    primaryContainer = VeridianPrimaryContainer,
    onPrimaryContainer = VeridianOnPrimaryContainer,
    background = Color(0xFFfaf9f8),
    surface = Color(0xFFfaf9f8),
    onSurface = Color(0xFF191C19),
    secondary = SlateSecondary,
    error = FinancialRedError
)

@Composable
fun ExpenseTrackerProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}