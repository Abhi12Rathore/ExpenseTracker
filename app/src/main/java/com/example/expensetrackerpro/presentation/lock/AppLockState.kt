package com.example.expensetrackerpro.presentation.lock

data class AppLockState(
    // False until the saved app lock settings have been read, so content never flashes before the lock
    val isLockStateKnown: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isLocked: Boolean = false,
    // True while the lock screen still has to open the authentication prompt
    val isPromptRequested: Boolean = false,
    val lockMessage: LockMessage? = null
)

/** Why the last unlock attempt did not unlock the app. */
enum class LockMessage {
    CANCELED,
    LOCKOUT,
    LOCKOUT_PERMANENT,
    ERROR
}
