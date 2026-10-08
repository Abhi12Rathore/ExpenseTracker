package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.time.TimeProvider
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Builds the range for two dates chosen in a date picker. Pickers report a date as midnight
 * UTC, so each one is re-read as a calendar date and applied as a whole day in the local zone.
 */
class GetCustomDateRangeUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    /** Covers [startUtcDayMillis]'s date through the end of [endUtcDayMillis]'s date. */
    operator fun invoke(startUtcDayMillis: Long, endUtcDayMillis: Long): DateRange {
        val start = localStartOfDay(startUtcDayMillis)
        val end = localStartOfDay(endUtcDayMillis).apply { add(Calendar.DAY_OF_MONTH, 1) }

        return DateRange(start.timeInMillis, end.timeInMillis)
    }

    private fun localStartOfDay(utcDayMillis: Long): Calendar {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcDayMillis
        }

        return timeProvider.now().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }
    }
}
