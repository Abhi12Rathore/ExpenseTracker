package com.example.expensetrackerpro.presentation.main.uistate

import com.example.expensetrackerpro.domain.models.ThemeMode

data class MainUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // False until the saved theme has been read, so the first frame is never drawn in the wrong one
    val isThemeLoaded: Boolean = false,
    // False while the database is still opening
    val isDataLoaded: Boolean = false
) {
    // The system splash stays up until this turns true
    val isReady: Boolean get() = isThemeLoaded && isDataLoaded
}
