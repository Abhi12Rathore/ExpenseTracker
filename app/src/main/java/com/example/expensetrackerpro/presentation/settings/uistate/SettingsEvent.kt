package com.example.expensetrackerpro.presentation.settings.uistate

import com.example.expensetrackerpro.domain.models.ThemeMode

sealed interface SettingsEvent {
    data object ThemeClicked : SettingsEvent
    data object ThemeDialogDismissed : SettingsEvent
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsEvent
}
