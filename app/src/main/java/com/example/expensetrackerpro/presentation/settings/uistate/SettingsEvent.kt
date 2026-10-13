package com.example.expensetrackerpro.presentation.settings.uistate

import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.domain.models.ThemeMode

sealed interface SettingsEvent {
    // Profile: the card opens a dialog that edits the name
    data object ProfileClicked : SettingsEvent
    data class ProfileNameChanged(val name: String) : SettingsEvent
    data object ProfileNameSaved : SettingsEvent
    data object ProfileDialogDismissed : SettingsEvent

    data class ThemeModeSelected(val mode: ThemeMode) : SettingsEvent

    // The snackbar shown after the app's language was changed
    data object LanguageUndoClicked : SettingsEvent
    data object LanguageChangeShown : SettingsEvent

    // The user may have changed their screen lock or the app's language in system settings while away
    data object ScreenResumed : SettingsEvent
    data class AppLockToggled(val enabled: Boolean) : SettingsEvent
    data object AppLockPromptLaunched : SettingsEvent
    data class AppLockAuthFinished(val result: AuthResult) : SettingsEvent
    data object AppLockErrorShown : SettingsEvent
    data object LockAfterClicked : SettingsEvent
    data object LockTimeoutDialogDismissed : SettingsEvent
    data class LockTimeoutSelected(val timeout: LockTimeout) : SettingsEvent

    // Danger zone: the delete only runs once the confirmation word has been typed
    data object DeleteAllDataClicked : SettingsEvent
    data class DeleteAllDataTextChanged(val text: String) : SettingsEvent
    data object DeleteAllDataConfirmed : SettingsEvent
    data object DeleteAllDataDismissed : SettingsEvent

    // Developer section, debug builds only. Loading asks before it runs.
    data object LoadSampleDataClicked : SettingsEvent
    data object LoadSampleDataConfirmed : SettingsEvent
    data object LoadSampleDataDismissed : SettingsEvent

    data object MessageShown : SettingsEvent
}
