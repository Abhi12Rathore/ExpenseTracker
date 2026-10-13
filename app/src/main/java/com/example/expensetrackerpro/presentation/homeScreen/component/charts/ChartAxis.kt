package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.SpendingBucket
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.utils.DateFormatter
import kotlin.math.ceil
import kotlin.math.roundToLong

/** The text under the bucket at [index]. */
internal data class AxisLabel(val index: Int, val text: String)

// In tenths. Multiples whose half is still a round number, since a gridline is drawn at half the maximum
private val NICE_STEPS = listOf(10L, 15L, 20L, 30L, 40L, 50L, 60L, 80L, 100L)

private const val DAYS_PER_LABEL = 7
private const val MAX_MONTH_LABELS = 12

/** The smallest round value at or above [value], used as the top of the y axis. */
internal fun niceAxisMax(value: Money): Money {
    if (!value.isPositive) return Money.ofRupees(1)

    // The largest power of ten that fits in the value
    var magnitude = 1L
    while (magnitude * 10 <= value.paise) magnitude *= 10
    val step = NICE_STEPS.first { it * magnitude >= value.paise * 10 }

    // Only a maximum of 1.5 paise is not whole, and it rounds up
    return Money((step * magnitude + 9) / 10)
}

/** The amount a gridline [fraction] of the way up an axis ending at this one stands for. */
internal fun Money.atFraction(fraction: Float): Money = Money((paise * fraction).roundToLong())

/**
 * A few labels rather than one per bar: every weekday for up to a week (`Mon` … `Sun`), every
 * seventh day beyond that (`1, 8, 15, 22, 29`), and month initials for months (`J` … `D`).
 */
internal fun xAxisLabels(chart: SpendingChart, dateFormatter: DateFormatter): List<AxisLabel> {
    val buckets = chart.buckets

    return when (chart.bucketSize) {
        BucketSize.DAY ->
            if (buckets.size <= DAYS_PER_LABEL) {
                buckets.labelEvery(1) { dateFormatter.formatWeekday(it.range.startMillis) }
            } else {
                buckets.labelEvery(DAYS_PER_LABEL) { dateFormatter.formatDayOfMonth(it.range.startMillis) }
            }
        BucketSize.MONTH -> {
            val step = ceil(buckets.size / MAX_MONTH_LABELS.toDouble()).toInt().coerceAtLeast(1)
            buckets.labelEvery(step) { dateFormatter.formatMonthInitial(it.range.startMillis) }
        }
    }
}

/** How a bucket is named in the tooltip and to screen readers: `Fri, 17 Oct` or `October 2026`. */
internal fun SpendingBucket.label(bucketSize: BucketSize, dateFormatter: DateFormatter): String = when (bucketSize) {
    BucketSize.DAY -> dateFormatter.formatWeekdayDayMonth(range.startMillis)
    BucketSize.MONTH -> dateFormatter.formatMonthYear(range.startMillis)
}

private fun List<SpendingBucket>.labelEvery(step: Int, text: (SpendingBucket) -> String): List<AxisLabel> =
    indices.filter { it % step == 0 }.map { AxisLabel(it, text(this[it])) }
