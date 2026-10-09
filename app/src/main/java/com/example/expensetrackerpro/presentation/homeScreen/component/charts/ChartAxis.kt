package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.SpendingBucket
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.utils.DateFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** The text under the bucket at [index]. */
internal data class AxisLabel(val index: Int, val text: String)

// Multiples whose half is still a round number, since a gridline is drawn at half the maximum
private val NICE_STEPS = listOf(1.0, 1.5, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)

private const val DAYS_PER_LABEL = 7
private const val MAX_MONTH_LABELS = 12

/** The smallest round value at or above [value], used as the top of the y axis. */
internal fun niceAxisMax(value: Double): Double {
    if (value <= 0) return 1.0

    val magnitude = 10.0.pow(floor(log10(value)))
    val normalized = value / magnitude
    // The tolerance keeps 3000 / 1000 from landing just above 3 and skipping to 4
    val step = NICE_STEPS.firstOrNull { it >= normalized - 1e-9 } ?: NICE_STEPS.last()

    return step * magnitude
}

/**
 * A few labels rather than one per bar: every weekday for up to a week (`Mon` … `Sun`), every
 * seventh day beyond that (`1, 8, 15, 22, 29`), and month initials for months (`J` … `D`).
 */
internal fun xAxisLabels(chart: SpendingChart): List<AxisLabel> {
    val buckets = chart.buckets

    return when (chart.bucketSize) {
        BucketSize.DAY ->
            if (buckets.size <= DAYS_PER_LABEL) {
                buckets.labelEvery(1) { DateFormatter.formatWeekday(it.range.startMillis) }
            } else {
                buckets.labelEvery(DAYS_PER_LABEL) { DateFormatter.formatDayOfMonth(it.range.startMillis) }
            }
        BucketSize.MONTH -> {
            val step = ceil(buckets.size / MAX_MONTH_LABELS.toDouble()).toInt().coerceAtLeast(1)
            buckets.labelEvery(step) { DateFormatter.formatMonthInitial(it.range.startMillis) }
        }
    }
}

/** How a bucket is named in the tooltip and to screen readers: `Fri, 17 Oct` or `October 2026`. */
internal fun SpendingBucket.label(bucketSize: BucketSize): String = when (bucketSize) {
    BucketSize.DAY -> DateFormatter.formatWeekdayDayMonth(range.startMillis)
    BucketSize.MONTH -> DateFormatter.formatMonthYear(range.startMillis)
}

private fun List<SpendingBucket>.labelEvery(step: Int, text: (SpendingBucket) -> String): List<AxisLabel> =
    indices.filter { it % step == 0 }.map { AxisLabel(it, text(this[it])) }
