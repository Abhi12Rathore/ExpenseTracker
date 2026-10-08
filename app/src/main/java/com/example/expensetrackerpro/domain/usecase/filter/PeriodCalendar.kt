package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.PeriodGranularity
import java.util.Calendar

/** The calendar field that one step of this granularity adds to. */
internal val PeriodGranularity.calendarField: Int
    get() = when (this) {
        PeriodGranularity.WEEK -> Calendar.WEEK_OF_YEAR
        PeriodGranularity.MONTH -> Calendar.MONTH
        PeriodGranularity.YEAR -> Calendar.YEAR
    }

/** Moves this calendar back to midnight on the first day of its week, month or year. */
internal fun Calendar.moveToStartOf(granularity: PeriodGranularity) {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)

    when (granularity) {
        PeriodGranularity.WEEK -> {
            val daysIntoWeek = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
            add(Calendar.DAY_OF_MONTH, -daysIntoWeek)
        }
        PeriodGranularity.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
        PeriodGranularity.YEAR -> set(Calendar.DAY_OF_YEAR, 1)
    }
    // Resolve the fields now so later add() calls start from the period start
    timeInMillis
}
