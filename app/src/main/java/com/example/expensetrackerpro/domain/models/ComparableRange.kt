package com.example.expensetrackerpro.domain.models

/** The stretch of the previous period that a period is fairly compared with. */
data class ComparableRange(
    /** The previous period, cut to as many days as the current one has had when [isPartial]. */
    val range: DateRange,
    /** The previous period from its first day to its last. */
    val wholePeriod: DateRange,
    /** True while the current period is still under way. */
    val isPartial: Boolean
)
