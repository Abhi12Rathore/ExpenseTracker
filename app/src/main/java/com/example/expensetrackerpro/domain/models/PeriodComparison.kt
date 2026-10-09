package com.example.expensetrackerpro.domain.models

/** How the selected period's balance compares with the period just before it. */
data class PeriodComparison(
    val granularity: PeriodGranularity,
    /** What the balance is compared with: the previous period, or its start up to the same day. */
    val previousRange: DateRange,
    /** This period's balance minus the previous one's: positive means more was saved. */
    val balanceDifference: Double,
    /** True when the selected period is still under way, so only part of the previous one counts. */
    val isPartial: Boolean
)
