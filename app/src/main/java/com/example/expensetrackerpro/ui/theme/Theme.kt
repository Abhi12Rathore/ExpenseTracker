package com.example.expensetrackerpro

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.expensetrackerpro.ui.theme.*

// Every role is set explicitly: anything left out falls back to the Material baseline purple
private val DarkColorScheme = darkColorScheme(
    primary = VeridianGreenPrimaryDark,
    onPrimary = VeridianOnPrimaryDark,
    primaryContainer = VeridianPrimaryContainerDark,
    onPrimaryContainer = VeridianOnPrimaryContainerDark,
    inversePrimary = VeridianGreenPrimary,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = CharcoalBackgroundDark,
    onBackground = OnBackgroundDark,
    surface = CharcoalSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = CharcoalSurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = VeridianGreenPrimaryDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    error = FinancialRedErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = Scrim,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = CharcoalSurfaceDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = VeridianGreenPrimary,
    onPrimary = VeridianOnPrimary,
    primaryContainer = VeridianPrimaryContainer,
    onPrimaryContainer = VeridianOnPrimaryContainer,
    inversePrimary = VeridianGreenPrimaryDark,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = VeridianGreenPrimary,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    error = FinancialRedError,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = Scrim,
    surfaceBright = SurfaceLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
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
            // Edge-to-edge: the bars are transparent, so only their icon contrast is set here
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}