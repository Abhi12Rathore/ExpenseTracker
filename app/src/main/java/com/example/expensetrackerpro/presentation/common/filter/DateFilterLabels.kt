package com.example.expensetrackerpro.presentation.common.filter

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.presentation.common.date.LocalDateFormatter
import com.example.expensetrackerpro.utils.DateFormatter

@StringRes
fun PeriodGranularity.labelRes(): Int = when (this) {
    PeriodGranularity.WEEK -> R.string.filter_week
    PeriodGranularity.MONTH -> R.string.filter_month
    PeriodGranularity.YEAR -> R.string.filter_year
}

// The range end is exclusive, so the last day shown is the one before it
private val PeriodFilterState.lastMillis: Long get() = dateRange.endMillis - 1

private fun PeriodFilterState.dayRange(dateFormatter: DateFormatter, withYear: Boolean): String =
    dateFormatter.formatDayRange(dateRange.startMillis, lastMillis, withYear)

/** The stepper's main line, e.g. `October 2026`, `2026` or `5 – 11 Oct 2026`. */
fun PeriodFilterState.title(dateFormatter: DateFormatter): String = when (val filter = filter) {
    is DateFilter.Custom -> dayRange(dateFormatter, withYear = true)
    is DateFilter.Period -> when (filter.granularity) {
        PeriodGranularity.WEEK -> dayRange(dateFormatter, withYear = true)
        PeriodGranularity.MONTH -> dateFormatter.formatMonthYear(dateRange.startMillis)
        PeriodGranularity.YEAR -> dateFormatter.formatYear(dateRange.startMillis)
    }
}

/** The days a named month or year covers, e.g. `1 – 31 Oct`. Null when the title already is the days. */
fun PeriodFilterState.subtitle(dateFormatter: DateFormatter): String? = when (val filter = filter) {
    is DateFilter.Custom -> null
    is DateFilter.Period -> when (filter.granularity) {
        PeriodGranularity.WEEK -> null
        PeriodGranularity.MONTH, PeriodGranularity.YEAR -> dayRange(dateFormatter, withYear = false)
    }
}

/** A compact name for the period, e.g. `Oct 2026`, for use inside other labels. */
fun PeriodFilterState.shortLabel(dateFormatter: DateFormatter): String = when (val filter = filter) {
    is DateFilter.Custom -> dayRange(dateFormatter, withYear = true)
    is DateFilter.Period -> when (filter.granularity) {
        PeriodGranularity.WEEK -> dayRange(dateFormatter, withYear = false)
        PeriodGranularity.MONTH -> dateFormatter.formatShortMonthYear(dateRange.startMillis)
        PeriodGranularity.YEAR -> dateFormatter.formatYear(dateRange.startMillis)
    }
}

/**
 * What a period is compared with, worded to follow "… than": `this time last month` while the
 * period is under way ([isPartial]), otherwise the previous period itself: `Aug`, `2025` or
 * `the week before`.
 */
@Composable
fun comparedWithLabel(
    granularity: PeriodGranularity,
    previousStartMillis: Long,
    isPartial: Boolean
): String = when (granularity) {
    PeriodGranularity.WEEK -> stringResource(
        if (isPartial) R.string.compared_with_this_time_last_week else R.string.compared_with_week_before
    )
    PeriodGranularity.MONTH ->
        if (isPartial) {
            stringResource(R.string.compared_with_this_time_last_month)
        } else {
            LocalDateFormatter.current.formatShortMonth(previousStartMillis)
        }
    PeriodGranularity.YEAR ->
        if (isPartial) {
            stringResource(R.string.compared_with_this_time_last_year)
        } else {
            LocalDateFormatter.current.formatYear(previousStartMillis)
        }
}
