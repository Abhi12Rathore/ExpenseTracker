package com.example.expensetrackerpro.presentation.common.filter

import androidx.annotation.StringRes
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.utils.DateFormatter

@StringRes
fun PeriodGranularity.labelRes(): Int = when (this) {
    PeriodGranularity.WEEK -> R.string.filter_week
    PeriodGranularity.MONTH -> R.string.filter_month
    PeriodGranularity.YEAR -> R.string.filter_year
}

// The range end is exclusive, so the last day shown is the one before it
private val PeriodFilterState.lastMillis: Long get() = dateRange.endMillis - 1

private fun PeriodFilterState.dayRange(withYear: Boolean): String =
    DateFormatter.formatDayRange(dateRange.startMillis, lastMillis, withYear)

/** The stepper's main line, e.g. `October 2026`, `2026` or `5 – 11 Oct 2026`. */
fun PeriodFilterState.title(): String = when (val filter = filter) {
    is DateFilter.Custom -> dayRange(withYear = true)
    is DateFilter.Period -> when (filter.granularity) {
        PeriodGranularity.WEEK -> dayRange(withYear = true)
        PeriodGranularity.MONTH -> DateFormatter.formatMonthYear(dateRange.startMillis)
        PeriodGranularity.YEAR -> DateFormatter.formatYear(dateRange.startMillis)
    }
}

/** The days a named month or year covers, e.g. `1 – 31 Oct`. Null when the title already is the days. */
fun PeriodFilterState.subtitle(): String? = when (val filter = filter) {
    is DateFilter.Custom -> null
    is DateFilter.Period -> when (filter.granularity) {
        PeriodGranularity.WEEK -> null
        PeriodGranularity.MONTH, PeriodGranularity.YEAR -> dayRange(withYear = false)
    }
}

/** A compact name for the period, e.g. `Oct 2026`, for use inside other labels. */
fun PeriodFilterState.shortLabel(): String = when (val filter = filter) {
    is DateFilter.Custom -> dayRange(withYear = true)
    is DateFilter.Period -> when (filter.granularity) {
        PeriodGranularity.WEEK -> dayRange(withYear = false)
        PeriodGranularity.MONTH -> DateFormatter.formatShortMonthYear(dateRange.startMillis)
        PeriodGranularity.YEAR -> DateFormatter.formatYear(dateRange.startMillis)
    }
}
