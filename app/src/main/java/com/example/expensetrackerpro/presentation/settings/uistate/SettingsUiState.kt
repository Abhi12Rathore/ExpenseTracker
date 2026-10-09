package com.example.expensetrackerpro.presentation.settings.uistate

import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.models.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isThemeDialogOpen: Boolean = false,
    val appLock: AppLockSettings = AppLockSettings(),
    val biometricStatus: BiometricStatus = BiometricStatus.AVAILABLE,
    val isLockTimeoutDialogOpen: Boolean = false,
    // True while the screen still has to open the prompt that confirms turning the lock on
    val isAuthRequested: Boolean = false,
    // Why turning the lock on failed, until the screen has shown it
    val appLockError: AuthResult? = null
) {
    // Without a screen lock there is nothing to unlock with
    val isAppLockAvailable: Boolean get() = biometricStatus != BiometricStatus.NO_DEVICE_CREDENTIAL
}
