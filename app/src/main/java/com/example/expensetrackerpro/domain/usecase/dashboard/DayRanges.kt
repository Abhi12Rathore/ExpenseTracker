package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateRange
import java.util.Calendar

/**
 * The consecutive days of [range], oldest first, measured on this calendar so each keeps its
 * real length. The calendar is moved in the process. [range] must be bounded.
 */
internal fun Calendar.dayRanges(range: DateRange): List<DateRange> {
    timeInMillis = range.startMillis

    return buildList {
        var startMillis = range.startMillis
        while (startMillis < range.endMillis) {
            add(Calendar.DAY_OF_MONTH, 1)
            val endMillis = minOf(timeInMillis, range.endMillis)
            add(DateRange(startMillis, endMillis))
            startMillis = endMillis
        }
    }
}

internal operator fun DateRange.contains(millis: Long): Boolean =
    millis >= startMillis && millis < endMillis
