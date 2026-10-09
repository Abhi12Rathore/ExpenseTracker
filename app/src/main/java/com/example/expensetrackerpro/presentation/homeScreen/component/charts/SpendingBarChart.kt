package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.SpendingBucket
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.ui.theme.extendedColors
import com.example.expensetrackerpro.utils.CurrencyFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val ChartHeight = 236.dp
// Kept clear above the tallest bar so the tooltip never covers one
private val TooltipHeadroom = 60.dp
private val AxisLabelHeight = 20.dp
private val BarGap = 2.dp
private val BarCornerRadius = 4.dp
private val MinBarHeight = 2.dp
private val FutureBarHeight = 6.dp
private val LabelSpacing = 2.dp

private const val RESTING_BAR_ALPHA = 0.45f
private const val FUTURE_BAR_ALPHA = 0.1f
// Labels sit on a patch of the card colour so bars and lines behind them don't cut through
private const val LABEL_BACKDROP_ALPHA = 0.85f
private const val BAR_GROW_MILLIS = 400
// How much later the last bar starts growing than the first
private const val STAGGER_MILLIS = 300

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

    val isPreview = LocalInspectionMode.current
    val growth = remember(periodKey) { Animatable(if (isPreview) 1f else 0f) }
    LaunchedEffect(periodKey) {
        growth.animateTo(1f, tween(BAR_GROW_MILLIS + STAGGER_MILLIS, easing = LinearEasing))
    }

    val barColor = MaterialTheme.extendedColors.expense
    val futureBarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = FUTURE_BAR_ALPHA)
    val gridColor = MaterialTheme.extendedColors.chartGrid
    val averageColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelBackdrop = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = LABEL_BACKDROP_ALPHA)

    // Text is laid out once per period rather than on every animation frame
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.extendedColors.chartAxisText)
    val axisMax = remember(chart) {
        niceAxisMax(max(buckets.maxOf { it.amount }, chart.average))
    }
    val xLabels = remember(periodKey, axisStyle) {
        xAxisLabels(chart).map { it.index to textMeasurer.measure(it.text, axisStyle) }
    }
    val gridLabels = remember(axisMax, axisStyle) {
        GridlineFractions.map { fraction ->
            fraction to textMeasurer.measure(CurrencyFormatter.formatShort(axisMax * fraction), axisStyle)
        }
    }
    val averageText = stringResource(
        when (chart.bucketSize) {
            BucketSize.DAY -> R.string.spending_average_per_day
            BucketSize.MONTH -> R.string.spending_average_per_month
        },
        CurrencyFormatter.formatWhole(chart.average)
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
                    bucket.amount > 0 -> {
                        val fullHeight = max(
                            (bucket.amount / axisMax).toFloat() * plotHeight,
                            MinBarHeight.toPx()
                        )
                        drawBar(
                            left = left,
                            bottom = plotBottom,
                            width = barWidth,
                            height = fullHeight * barGrowth(growth.value, index, buckets.size),
                            color = barColor.copy(
                                alpha = if (index == selectedIndex) 1f else RESTING_BAR_ALPHA
                            )
                        )
                    }
                }
            }

            if (chart.average > 0) {
                val y = plotBottom - (chart.average / axisMax).toFloat() * plotHeight
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
                    bucket.label(chart.bucketSize),
                    CurrencyFormatter.format(bucket.amount)
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
                SpendingTooltip(
                    bucket = bucket,
                    bucketSize = chart.bucketSize,
                    onClick = { onBucketClick(bucket) },
                    modifier = Modifier.centeredOn((index + 0.5f) / buckets.size)
                )
            }
        }
    }
}

@Composable
private fun SpendingTooltip(
    bucket: SpendingBucket,
    bucketSize: BucketSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val openHistory = stringResource(R.string.spending_tooltip_open_history)

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClickLabel = openHistory, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.spending_tooltip,
                    CurrencyFormatter.format(bucket.amount),
                    bucket.label(bucketSize),
                    pluralStringResource(
                        R.plurals.transaction_count,
                        bucket.transactionCount,
                        bucket.transactionCount
                    )
                ),
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Places the content at the top, centred on [fraction] of the width but kept inside the edges. */
private fun Modifier.centeredOn(fraction: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val maxLeft = max(0, constraints.maxWidth - placeable.width)
    val left = (constraints.maxWidth * fraction - placeable.width / 2f).roundToInt().coerceIn(0, maxLeft)

    layout(constraints.maxWidth, placeable.height) {
        placeable.place(left, 0)
    }
}

/** How far the bar at [index] has grown (0 to 1) at [progress] of the whole staggered animation. */
private fun barGrowth(progress: Float, index: Int, count: Int): Float {
    val delay = if (count > 1) STAGGER_MILLIS * index / (count - 1f) else 0f
    val elapsed = progress * (BAR_GROW_MILLIS + STAGGER_MILLIS) - delay

    return FastOutSlowInEasing.transform((elapsed / BAR_GROW_MILLIS).coerceIn(0f, 1f))
}

/** Text on a rounded patch of [backdrop]. */
private fun DrawScope.drawLabel(
    label: TextLayoutResult,
    topLeft: Offset,
    backdrop: Color,
    alpha: Float = 1f
) {
    val padding = LabelSpacing.toPx()
    drawRoundRect(
        color = backdrop,
        topLeft = Offset(topLeft.x - padding, topLeft.y),
        size = Size(label.size.width + 2 * padding, label.size.height.toFloat()),
        cornerRadius = CornerRadius(padding),
        alpha = alpha
    )
    drawText(label, topLeft = topLeft, alpha = alpha)
}

/** A bar standing on [bottom] with only its top corners rounded. */
private fun DrawScope.drawBar(left: Float, bottom: Float, width: Float, height: Float, color: Color) {
    if (height <= 0f) return

    val radius = CornerRadius(min(BarCornerRadius.toPx(), min(width / 2f, height)))
    val bar = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = bottom - height,
                right = left + width,
                bottom = bottom,
                topLeftCornerRadius = radius,
                topRightCornerRadius = radius
            )
        )
    }
    drawPath(bar, color)
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
