package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.presentation.common.currency.amountFormatKey
import com.example.expensetrackerpro.presentation.common.date.LocalDateFormatter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.SpendingBucket
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.ui.theme.extendedColors
import kotlin.math.max

private val ChartHeight = 236.dp
// Kept clear above the tallest bar so the tooltip never covers one
private val TooltipHeadroom = 60.dp
private val AxisLabelHeight = 20.dp
private val BarGap = 2.dp
private val MinBarHeight = 2.dp

private const val RESTING_BAR_ALPHA = 0.45f

// Gridlines are drawn at these shares of the axis maximum, plus the baseline
private val GridlineFractions = listOf(0.5f, 1f)

/** Daily or monthly expenses of the selected period, with its loading and empty states. */
@Composable
fun SpendingCard(
    chart: SpendingChart,
    isLoading: Boolean,
    onBucketClick: (SpendingBucket) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (chart.bucketSize) {
        BucketSize.DAY -> stringResource(R.string.spending_daily_title)
        BucketSize.MONTH -> stringResource(R.string.spending_monthly_title)
    }

    ChartCard(title = title, modifier = modifier) {
        when {
            isLoading -> SpendingBarsShimmer()
            !chart.hasSpending -> ChartEmptyMessage(stringResource(R.string.spending_empty))
            else -> SpendingBarChart(chart = chart, onBucketClick = onBucketClick)
        }
    }
}

/**
 * One bar per bucket. Tapping a bar selects it and dragging across the chart scrubs the
 * selection; the selected bar gets a tooltip, and tapping that calls [onBucketClick].
 */
@Composable
fun SpendingBarChart(
    chart: SpendingChart,
    onBucketClick: (SpendingBucket) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = LocalCurrencyFormatter.current
    // Labels are kept between frames, so a change of currency has to write them again
    val formatKey = amountFormatKey()
    val buckets = chart.buckets
    if (buckets.isEmpty()) return

    // Names the period on show, not its amounts: saving a transaction must not replay the animation
    val periodKey = Triple(buckets.first().range, buckets.size, chart.bucketSize)

    var selectedIndex by remember(periodKey) { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current
    val lastSelectableIndex by rememberUpdatedState(buckets.indexOfLast { !it.isFuture })

    val select: (Int?) -> Unit = { index ->
        if (index != selectedIndex) {
            selectedIndex = index
            if (index != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    // The bucket under a touch at x, never one that hasn't started
    val indexAt: (Float, Int) -> Int? = { x, width ->
        if (lastSelectableIndex < 0 || width <= 0) {
            null
        } else {
            (x / width * buckets.size).toInt().coerceIn(0, lastSelectableIndex)
        }
    }

    val startsDrawn = chartStartsDrawn()
    val growth = remember(periodKey) { Animatable(if (startsDrawn) 1f else 0f) }
    LaunchedEffect(periodKey) {
        growth.animateTo(1f, tween(STAGGERED_GROW_MILLIS, easing = LinearEasing))
    }

    val barColor = MaterialTheme.extendedColors.expense
    val futureBarColor = futureBarColor()
    val gridColor = MaterialTheme.extendedColors.chartGrid
    val averageColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelBackdrop = labelBackdropColor()

    // Text is laid out once per period rather than on every animation frame
    val dateFormatter = LocalDateFormatter.current
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.extendedColors.chartAxisText)
    val axisMax = remember(chart) {
        niceAxisMax(maxOf(buckets.maxOf { it.amount }, chart.average))
    }
    val xLabels = remember(periodKey, axisStyle) {
        xAxisLabels(chart, dateFormatter).map { it.index to textMeasurer.measure(it.text, axisStyle) }
    }
    val gridLabels = remember(axisMax, axisStyle, formatKey) {
        GridlineFractions.map { fraction ->
            fraction to textMeasurer.measure(currencyFormatter.formatShort(axisMax.atFraction(fraction)), axisStyle)
        }
    }
    val averageText = stringResource(
        when (chart.bucketSize) {
            BucketSize.DAY -> R.string.spending_average_per_day
            BucketSize.MONTH -> R.string.spending_average_per_month
        },
        currencyFormatter.formatWhole(chart.average)
    )
    val averageLabel = remember(averageText, axisStyle, averageColor) {
        textMeasurer.measure(
            averageText,
            axisStyle.copy(color = averageColor, fontWeight = FontWeight.Medium)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .pointerInput(periodKey) {
                detectTapGestures { offset ->
                    val index = indexAt(offset.x, size.width)
                    select(if (index == selectedIndex) null else index)
                }
            }
            .pointerInput(periodKey) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> select(indexAt(offset.x, size.width)) }
                ) { change, _ ->
                    change.consume()
                    select(indexAt(change.position.x, size.width))
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val plotTop = TooltipHeadroom.toPx()
            val plotBottom = size.height - AxisLabelHeight.toPx()
            val plotHeight = plotBottom - plotTop
            val slotWidth = size.width / buckets.size
            val gap = BarGap.toPx()
            val barWidth = (slotWidth - gap).coerceAtLeast(1f)
            val hairline = 1.dp.toPx()
            val labelSpacing = LabelSpacing.toPx()

            drawLine(gridColor, Offset(0f, plotBottom), Offset(size.width, plotBottom), hairline)
            gridLabels.forEach { (fraction, label) ->
                val y = plotBottom - plotHeight * fraction
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), hairline)
            }

            buckets.forEachIndexed { index, bucket ->
                val left = index * slotWidth + gap / 2
                when {
                    bucket.isFuture ->
                        drawBar(left, plotBottom, barWidth, FutureBarHeight.toPx(), futureBarColor)
                    bucket.amount.isPositive -> {
                        val fullHeight = max(
                            bucket.amount.ratioTo(axisMax) * plotHeight,
                            MinBarHeight.toPx()
                        )
                        drawBar(
                            left = left,
                            baseline = plotBottom,
                            width = barWidth,
                            height = fullHeight * barGrowth(growth.value, index, buckets.size),
                            color = barColor.copy(
                                alpha = if (index == selectedIndex) 1f else RESTING_BAR_ALPHA
                            )
                        )
                    }
                }
            }

            if (chart.average.isPositive) {
                val y = plotBottom - chart.average.ratioTo(axisMax) * plotHeight
                val dash = 4.dp.toPx()
                drawLine(
                    color = averageColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = hairline,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                    alpha = growth.value
                )
                drawLabel(
                    label = averageLabel,
                    topLeft = Offset(
                        x = size.width - averageLabel.size.width,
                        y = y - averageLabel.size.height - labelSpacing
                    ),
                    backdrop = labelBackdrop,
                    alpha = growth.value
                )
            }

            // Drawn last so they stay readable over the bars and the average line
            gridLabels.forEach { (fraction, label) ->
                val y = plotBottom - plotHeight * fraction
                drawLabel(
                    label = label,
                    topLeft = Offset(0f, y - label.size.height - labelSpacing),
                    backdrop = labelBackdrop
                )
            }

            xLabels.forEach { (index, label) ->
                val centerX = (index + 0.5f) * slotWidth
                val maxLeft = max(0f, size.width - label.size.width)
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(
                        x = (centerX - label.size.width / 2f).coerceIn(0f, maxLeft),
                        y = plotBottom + labelSpacing * 2
                    )
                )
            }
        }

        // The canvas has no children, so each bar gets an invisible node for screen readers
        Row(modifier = Modifier.matchParentSize()) {
            buckets.forEachIndexed { index, bucket ->
                val description = stringResource(
                    R.string.spending_bar_description,
                    bucket.label(chart.bucketSize, dateFormatter),
                    currencyFormatter.format(bucket.amount)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            contentDescription = description
                            if (!bucket.isFuture) {
                                onClick {
                                    select(index)
                                    true
                                }
                            }
                        }
                )
            }
        }

        selectedIndex?.let { index ->
            buckets.getOrNull(index)?.let { bucket ->
                ChartTooltip(
                    text = stringResource(
                        R.string.spending_tooltip,
                        currencyFormatter.format(bucket.amount),
                        bucket.label(chart.bucketSize, dateFormatter),
                        pluralStringResource(
                            R.plurals.transaction_count,
                            bucket.transactionCount,
                            bucket.transactionCount
                        )
                    ),
                    modifier = Modifier.centeredOn((index + 0.5f) / buckets.size),
                    onClick = { onBucketClick(bucket) },
                    onClickLabel = stringResource(R.string.spending_tooltip_open_transactions)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SpendingCardDailyPreview() {
    ExpenseTrackerProTheme {
        SpendingCard(chart = PreviewData.dailySpending, isLoading = false, onBucketClick = {})
    }
}

@PreviewLightDark
@Composable
private fun SpendingCardMonthlyPreview() {
    ExpenseTrackerProTheme {
        SpendingCard(chart = PreviewData.monthlySpending, isLoading = false, onBucketClick = {})
    }
}

@PreviewLightDark
@Composable
private fun SpendingCardLoadingPreview() {
    ExpenseTrackerProTheme {
        SpendingCard(chart = SpendingChart(), isLoading = true, onBucketClick = {})
    }
}

@PreviewLightDark
@Composable
private fun SpendingCardEmptyPreview() {
    ExpenseTrackerProTheme {
        SpendingCard(chart = SpendingChart(), isLoading = false, onBucketClick = {})
    }
}
