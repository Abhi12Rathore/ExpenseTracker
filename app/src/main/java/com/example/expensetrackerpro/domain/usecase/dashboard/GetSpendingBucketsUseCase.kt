package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.SpendingBucket
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import com.example.expensetrackerpro.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

/**
 * Expenses of [DateRange] as one bucket per day, or per calendar month once the range is
 * longer than [MAX_DAILY_BUCKETS] days. Every day or month of the range gets a bucket, also
 * the ones nothing was spent in.
 */
class GetSpendingBucketsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val timeProvider: TimeProvider
) {

    /** [range] must be bounded and start at a local midnight. */
    operator fun invoke(range: DateRange): Flow<SpendingChart> =
        dashboardRepository.getDailyExpenses(range).map { days -> buildChart(range, days) }

    private fun buildChart(range: DateRange, days: List<DailyExpense>): SpendingChart {
        val bucketSize = bucketSizeFor(range)
        val nowMillis = timeProvider.now().timeInMillis

        val buckets = bucketRanges(range, bucketSize).map { bucket ->
            val spent = days.filter {
                it.dateMillis >= bucket.startMillis && it.dateMillis < bucket.endMillis
            }
            SpendingBucket(
                range = bucket,
                amount = spent.sumOf { it.amount },
                transactionCount = spent.sumOf { it.transactionCount },
                isFuture = bucket.startMillis > nowMillis
            )
        }
        val started = buckets.filterNot { it.isFuture }

        return SpendingChart(
            bucketSize = bucketSize,
            buckets = buckets,
            average = if (started.isEmpty()) 0.0 else started.sumOf { it.amount } / started.size
        )
    }

    private fun bucketSizeFor(range: DateRange): BucketSize {
        val lastDailyEnd = timeProvider.now().apply {
            timeInMillis = range.startMillis
            add(Calendar.DAY_OF_MONTH, MAX_DAILY_BUCKETS)
        }
        return if (range.endMillis <= lastDailyEnd.timeInMillis) BucketSize.DAY else BucketSize.MONTH
    }

    // Stepping a calendar rather than adding fixed millis keeps days and months their real length
    private fun bucketRanges(range: DateRange, bucketSize: BucketSize): List<DateRange> {
        val cursor = timeProvider.now().apply { timeInMillis = range.startMillis }

        return buildList {
            var startMillis = range.startMillis
            while (startMillis < range.endMillis) {
                when (bucketSize) {
                    BucketSize.DAY -> cursor.add(Calendar.DAY_OF_MONTH, 1)
                    BucketSize.MONTH -> {
                        // A range may start mid-month: its first bucket still ends with the month
                        cursor.set(Calendar.DAY_OF_MONTH, 1)
                        cursor.add(Calendar.MONTH, 1)
                    }
                }
                val endMillis = minOf(cursor.timeInMillis, range.endMillis)
                add(DateRange(startMillis, endMillis))
                startMillis = endMillis
            }
        }
    }

    companion object {
        const val MAX_DAILY_BUCKETS = 62
    }
}
