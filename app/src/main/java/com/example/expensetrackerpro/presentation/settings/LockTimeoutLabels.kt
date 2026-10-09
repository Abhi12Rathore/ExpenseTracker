package com.example.expensetrackerpro.presentation.settings

import androidx.annotation.StringRes
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.LockTimeout

@StringRes
fun LockTimeout.labelRes(): Int = when (this) {
    LockTimeout.IMMEDIATELY -> R.string.lock_timeout_immediately
    LockTimeout.SECONDS_30 -> R.string.lock_timeout_30_seconds
    LockTimeout.MINUTE_1 -> R.string.lock_timeout_1_minute
    LockTimeout.MINUTES_5 -> R.string.lock_timeout_5_minutes
}
