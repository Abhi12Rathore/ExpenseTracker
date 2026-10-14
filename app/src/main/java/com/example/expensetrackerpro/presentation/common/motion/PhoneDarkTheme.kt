package com.example.expensetrackerpro.presentation.common.motion

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the phone itself is in dark mode, or null when that can't be told.
 *
 * Not the same as `isSystemInDarkTheme()`: from Android 12 the app sets its own night mode
 * (`setApplicationNightMode`, for the splash), and every configuration the app can read then
 * reports that instead of the phone's. The phone's own setting is still in [UiModeManager], but
 * a schedule (Auto, or custom hours) doesn't say whether it is dark right now.
 */
fun Context.phoneDarkTheme(): Boolean? {
    val appConfiguration = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    // Before Android 12 the app has no night mode of its own, so its configuration is the phone's
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return appConfiguration == Configuration.UI_MODE_NIGHT_YES

    return when (getSystemService(UiModeManager::class.java).nightMode) {
        UiModeManager.MODE_NIGHT_YES -> true
        UiModeManager.MODE_NIGHT_NO -> false
        else -> null
    }
}

/**
 * The phone's dark mode, for drawing the app while it follows the phone. Where the phone's setting
 * can't be told, the app's configuration is used, which is the phone's once the app follows it.
 */
@Composable
fun phoneInDarkTheme(): Boolean {
    val configuration = LocalConfiguration.current
    val appNight = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    // The configuration is read above so that a change of the phone's dark mode recomposes the caller
    return LocalContext.current.phoneDarkTheme() ?: appNight
}
