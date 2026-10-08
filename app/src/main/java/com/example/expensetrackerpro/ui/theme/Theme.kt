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
import com.example.expensetrackerpro.ui.theme.OnSecondaryContainerDark
import com.example.expensetrackerpro.ui.theme.OnSecondaryContainerLight
import com.example.expensetrackerpro.ui.theme.OutlineVariantDark
import com.example.expensetrackerpro.ui.theme.OutlineVariantLight
import com.example.expensetrackerpro.ui.theme.SecondaryContainerDark
import com.example.expensetrackerpro.ui.theme.SecondaryContainerLight
import com.example.expensetrackerpro.ui.theme.SlateSecondary
import com.example.expensetrackerpro.ui.theme.SurfaceContainerDark
import com.example.expensetrackerpro.ui.theme.SurfaceContainerHighDark
import com.example.expensetrackerpro.ui.theme.SurfaceContainerHighLight
import com.example.expensetrackerpro.ui.theme.SurfaceContainerHighestDark
import com.example.expensetrackerpro.ui.theme.SurfaceContainerHighestLight
import com.example.expensetrackerpro.ui.theme.SurfaceContainerLight
import com.example.expensetrackerpro.ui.theme.SurfaceContainerLowDark
import com.example.expensetrackerpro.ui.theme.SurfaceContainerLowLight
import com.example.expensetrackerpro.ui.theme.SurfaceContainerLowestDark
import com.example.expensetrackerpro.ui.theme.SurfaceContainerLowestLight
import com.example.expensetrackerpro.ui.theme.SurfaceLight
import com.example.expensetrackerpro.ui.theme.Typography
import com.example.expensetrackerpro.ui.theme.VeridianGreenPrimary
import com.example.expensetrackerpro.ui.theme.VeridianGreenPrimaryDark
import com.example.expensetrackerpro.ui.theme.VeridianOnPrimary
import com.example.expensetrackerpro.ui.theme.VeridianOnPrimaryContainer
import com.example.expensetrackerpro.ui.theme.VeridianOnPrimaryDark
import com.example.expensetrackerpro.ui.theme.VeridianPrimaryContainer
import com.example.expensetrackerpro.ui.theme.VeridianPrimaryContainerDark

private val DarkColorScheme = darkColorScheme(
    primary = VeridianGreenPrimaryDark,
    onPrimary = VeridianOnPrimaryDark,
    primaryContainer = VeridianPrimaryContainerDark,
    background = CharcoalBackgroundDark,
    surface = CharcoalSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = CharcoalSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outlineVariant = OutlineVariantDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = VeridianGreenPrimary,
    onPrimary = VeridianOnPrimary,
    primaryContainer = VeridianPrimaryContainer,
    onPrimaryContainer = VeridianOnPrimaryContainer,
    background = SurfaceLight,
    surface = SurfaceLight,
    onSurface = Color(0xFF191C19),
    secondary = SlateSecondary,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outlineVariant = OutlineVariantLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    error = FinancialRedError
)

// The palette is fixed: wallpaper-based dynamic color is deliberately not used
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