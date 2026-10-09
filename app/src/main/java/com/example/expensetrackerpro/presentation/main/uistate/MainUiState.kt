package com.example.expensetrackerpro.presentation.main.uistate

import com.example.expensetrackerpro.domain.models.ThemeMode

data class MainUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // False until the saved theme has been read, so the first frame is never drawn in the wrong one
    val isThemeLoaded: Boolean = false,
    // False while the database is still opening
    val isDataLoaded: Boolean = false,
    // False until the saved app lock settings have been read, so content never flashes before the lock
    val isLockStateKnown: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isLocked: Boolean = false,
    // True while the lock screen still has to open the authentication prompt
    val isPromptRequested: Boolean = false,
    val lockMessage: LockMessage? = null
) {
    // The system splash stays up until this turns true
    val isReady: Boolean get() = isThemeLoaded && isDataLoaded && isLockStateKnown
}

/** Why the last unlock attempt did not unlock the app. */
enum class LockMessage {
    CANCELED,
    LOCKOUT,
    LOCKOUT_PERMANENT,
    ERROR
}
