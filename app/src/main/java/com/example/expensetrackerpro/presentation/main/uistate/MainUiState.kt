package com.example.expensetrackerpro.presentation.main.uistate

import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.presentation.lock.LockMessage

data class MainUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // False until the saved theme has been read, so the first frame is never drawn in the wrong one
    val isThemeLoaded: Boolean = false,
    // False while the database is still opening
    val isDataLoaded: Boolean = false,
    // The app lock, as its AppLockController reports it
    val isLockStateKnown: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isLocked: Boolean = false,
    val isPromptRequested: Boolean = false,
    val lockMessage: LockMessage? = null
) {
    // The system splash stays up until this turns true
    val isReady: Boolean get() = isThemeLoaded && isDataLoaded && isLockStateKnown
}
