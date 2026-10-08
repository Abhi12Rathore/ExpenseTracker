package com.example.expensetrackerpro.domain.models

enum class PeriodGranularity { WEEK, MONTH, YEAR }

/** What a screen is filtered to: a calendar period that can be stepped through, or a picked range. */
sealed interface DateFilter {

    /** The week, month or year that contains [anchorMillis]. */
    data class Period(
        val granularity: PeriodGranularity,
        val anchorMillis: Long
    ) : DateFilter

    data class Custom(val range: DateRange) : DateFilter
}
