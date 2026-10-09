package com.example.expensetrackerpro.presentation.settings.uistate

import com.example.expensetrackerpro.domain.models.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isThemeDialogOpen: Boolean = false
)
