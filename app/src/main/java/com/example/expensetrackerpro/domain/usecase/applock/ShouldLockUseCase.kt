package com.example.expensetrackerpro.domain.usecase.applock

import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.time.TimeProvider
import javax.inject.Inject

class ShouldLockUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    /** [lastBackgroundedAt] is the epoch millis the app last left the foreground, or null if it never has. */
    operator fun invoke(settings: AppLockSettings, lastBackgroundedAt: Long?): Boolean {
        if (!settings.enabled || lastBackgroundedAt == null) return false
        val elapsed = timeProvider.now().timeInMillis - lastBackgroundedAt
        // A clock that moved backwards can't be trusted to measure the timeout, so lock
        return elapsed < 0 || elapsed >= settings.timeout.millis
    }
}
