package com.example.expensetrackerpro.presentation.main.uistate

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.presentation.lock.LockMessage

data class MainUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // False until the saved theme has been read, so the first frame is never drawn in the wrong one
    val isThemeLoaded: Boolean = false,
    // The currency amounts are shown in; read under the splash with the data
    val currency: AppCurrency = AppCurrency.INR,
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

    /**
     * What the activity shows. Nothing until the lock state is known, so the app's screens, and
     * whatever a shortcut's link opens with them (the scanner, the voice), never run before the
     * lock screen.
     */
    val content: MainContent
        get() = when {
            !isLockStateKnown -> MainContent.NONE
            isLocked -> MainContent.LOCK_SCREEN
            else -> MainContent.APP
        }
}

enum class MainContent {
    /** The lock state is still being read: nothing is composed. */
    NONE,
    LOCK_SCREEN,

    /** The app's screens, behind the NavHost. */
    APP
}
