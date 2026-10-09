package com.example.expensetrackerpro.domain.models

enum class BucketSize { DAY, MONTH }

/** The expenses of one day or calendar month of the selected range. */
data class SpendingBucket(
    val range: DateRange,
    val amount: Double,
    val transactionCount: Int,
    // Starts after the current moment, so nothing can have been spent in it yet
    val isFuture: Boolean
)

/** Expenses of a range split into consecutive buckets, oldest first, with no gaps. */
data class SpendingChart(
    val bucketSize: BucketSize = BucketSize.DAY,
    val buckets: List<SpendingBucket> = emptyList(),
    /** Mean spend per bucket that has started, or 0 when none has. */
    val average: Double = 0.0
) {
    val hasSpending: Boolean get() = buckets.any { it.amount > 0 }
}
