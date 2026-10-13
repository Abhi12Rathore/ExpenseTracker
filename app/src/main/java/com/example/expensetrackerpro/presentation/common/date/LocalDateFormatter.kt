package com.example.expensetrackerpro.presentation.common.date

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.expensetrackerpro.data.time.SystemTimeProvider
import com.example.expensetrackerpro.utils.DateFormatter
import java.util.Locale

/**
 * The app's [DateFormatter], for Composables. MainActivity provides the injected one, which
 * follows the app's language. The default is only ever used by @Preview, which has no activity:
 * it writes dates in English, on the 12-hour clock.
 */
val LocalDateFormatter = staticCompositionLocalOf {
    DateFormatter(
        localeProvider = { Locale.ENGLISH },
        timeProvider = SystemTimeProvider(),
        hourFormatProvider = { false }
    )
}
