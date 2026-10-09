package com.example.expensetrackerpro.domain.models

/** How the selected period's balance compares with the period just before it. */
data class PeriodComparison(
    val granularity: PeriodGranularity,
    val previousRange: DateRange,
    /** This period's balance minus the previous one's: positive means more was saved. */
    val balanceDifference: Double
)
