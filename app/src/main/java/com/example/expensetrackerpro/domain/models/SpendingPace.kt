package com.example.expensetrackerpro.domain.models

/** Everything spent from the start of a period up to and including the day of [dateMillis]. */
data class PacePoint(
    val dateMillis: Long,
    val total: Money
)

/** How fast a week or month is being spent compared with the one before it. */
data class SpendingPace(
    val granularity: PeriodGranularity,
    /** One point per day of the selected period that has started, oldest first. */
    val current: List<PacePoint>,
    /** One point per day of the whole previous period, oldest first. */
    val previous: List<PacePoint>,
    /** Days in the longer of the two periods, so both fit on one axis. */
    val dayCount: Int,
    /** Spent so far minus what the previous period had spent by the same day. */
    val difference: Money,
    /** True while the selected period is still under way. */
    val isPartial: Boolean
) {
    val hasSpending: Boolean
        get() = current.lastOrNull()?.total?.isPositive == true || previous.lastOrNull()?.total?.isPositive == true
}
