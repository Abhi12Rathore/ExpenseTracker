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
    val appLockError: AuthResult? = null,
    // Debug builds only: the section that loads and clears test data
    val isDeveloperSectionVisible: Boolean = false,
    // Non-null while its confirmation dialog is showing
    val pendingDeveloperAction: DeveloperAction? = null,
    val isDeveloperActionRunning: Boolean = false,
    // How the last developer action ended, until the screen has shown it
    val developerMessage: DeveloperMessage? = null
) {
    // Without a screen lock there is nothing to unlock with
    val isAppLockAvailable: Boolean get() = biometricStatus != BiometricStatus.NO_DEVICE_CREDENTIAL
}

enum class DeveloperAction { LOAD_SAMPLE_DATA, CLEAR_ALL_DATA }

sealed interface DeveloperMessage {
    data class SampleDataLoaded(val transactionCount: Int) : DeveloperMessage
    data object AllDataCleared : DeveloperMessage
    data object Failed : DeveloperMessage
}
