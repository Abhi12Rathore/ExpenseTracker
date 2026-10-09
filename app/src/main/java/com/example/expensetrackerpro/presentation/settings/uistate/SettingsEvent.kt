package com.example.expensetrackerpro.presentation.settings.uistate

import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.domain.models.ThemeMode

sealed interface SettingsEvent {
    data object ThemeClicked : SettingsEvent
    data object ThemeDialogDismissed : SettingsEvent
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsEvent

    // The user may have changed their screen lock in system settings while away
    data object ScreenResumed : SettingsEvent
    data class AppLockToggled(val enabled: Boolean) : SettingsEvent
    data object AppLockPromptLaunched : SettingsEvent
    data class AppLockAuthFinished(val result: AuthResult) : SettingsEvent
    data object AppLockErrorShown : SettingsEvent
    data object LockAfterClicked : SettingsEvent
    data object LockTimeoutDialogDismissed : SettingsEvent
    data class LockTimeoutSelected(val timeout: LockTimeout) : SettingsEvent
}
