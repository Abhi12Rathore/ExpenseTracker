package com.example.expensetrackerpro.domain.models

data class AppLockSettings(
    val enabled: Boolean = false,
    val timeout: LockTimeout = LockTimeout.SECONDS_30
)

/** How long the app may stay in the background before it locks. */
enum class LockTimeout(val millis: Long) {
    IMMEDIATELY(0L),
    SECONDS_30(30_000L),
    MINUTE_1(60_000L),
    MINUTES_5(300_000L)
}
