package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.PacePoint
import com.example.expensetrackerpro.domain.models.SpendingPace
import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.presentation.common.filter.comparedWithLabel
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.ui.theme.extendedColors
import com.example.expensetrackerpro.presentation.common.date.LocalDateFormatter
import com.example.expensetrackerpro.utils.DateFormatter
import kotlin.math.max
import kotlin.math.roundToInt

private val ChartHeight = 220.dp
// Kept clear above the lines so the tooltip never covers them
private val TooltipHeadroom = 48.dp
private val AxisLabelHeight = 20.dp
private val LineWidth = 2.dp
private val DotRadius = 4.dp
private val DashLength = 4.dp

private const val PREVIOUS_LINE_ALPHA = 0.5f
private const val AREA_ALPHA = 0.2f
private const val DRAW_MILLIS = 700
private const val DAYS_PER_LABEL = 7

// Gridlines are drawn at these shares of the axis maximum, plus the baseline
private val GridlineFractions = listOf(0.5f, 1f)

/** Cumulative spending of a week or month against the one before, with loading and empty states. */
@Composable
fun SpendingPaceCard(
    pace: SpendingPace?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    ChartCard(title = stringResource(R.string.pace_title), modifier = modifier) {
        when {
            isLoading -> ChartBlockShimmer()
            pace == null || !pace.hasSpending -> ChartEmptyMessage(stringResource(R.string.pace_empty))
            else -> {
                val summary = paceSummary(pace)

                PaceChip(text = summary)
                Spacer(modifier = Modifier.height(8.dp))
                SpendingPaceChart(pace = pace, description = summary)
                ChartLegend {
                    ChartLegendItem(
                        color = MaterialTheme.colorScheme.primary,
                        label = stringResource(R.string.pace_legend_current)
                    )
                    ChartLegendItem(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PREVIOUS_LINE_ALPHA),
                        label = stringResource(R.string.pace_legend_previous)
                    )
                }
            }
        }
    }
}

/** e.g. `₹2,540 less than this time last month` */
@Composable
private fun paceSummary(pace: SpendingPace): String {
    val currencyFormatter = LocalCurrencyFormatter.current
    val comparedWith = comparedWithLabel(
        granularity = pace.granularity,
        previousStartMillis = pace.previous.firstOrNull()?.dateMillis ?: 0L,
        isPartial = pace.isPartial
    )
    val amount = currencyFormatter.formatWhole(pace.difference.abs())

    return when {
        // Less than a rupee apart reads as no difference
        pace.difference.roundedToRupees().isZero -> stringResource(R.string.pace_same, comparedWith)
        pace.difference.isNegative -> stringResource(R.string.pace_less, amount, comparedWith)
        else -> stringResource(R.string.pace_more, amount, comparedWith)
    }
}

@Composable
private fun PaceChip(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * Two cumulative lines on one day axis: the selected period solid with a filled area and a
 * labelled end dot, the previous period dashed. Tapping or dragging picks a day and shows what
 * each period had spent by then.
 */
@Composable
fun SpendingPaceChart(
    pace: SpendingPace,
    description: String,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = LocalCurrencyFormatter.current
    val dayCount = pace.dayCount
    if (dayCount < 2) return

    // Names the periods on show, not their amounts: saving a transaction must not replay the animation
    val periodKey = Triple(pace.previous.firstOrNull()?.dateMillis, dayCount, pace.granularity)

    var selectedIndex by remember(periodKey) { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current
    val select: (Int?) -> Unit = { index ->
        if (index != selectedIndex) {
            selectedIndex = index
            if (index != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    // The day nearest a touch at x
    val indexAt: (Float, Int) -> Int = { x, width ->
        if (width <= 0) 0 else (x / width * (dayCount - 1)).roundToInt().coerceIn(0, dayCount - 1)
    }

    val startsDrawn = chartStartsDrawn()
    val reveal = remember(periodKey) { Animatable(if (startsDrawn) 1f else 0f) }
    LaunchedEffect(periodKey) {
        reveal.animateTo(1f, tween(DRAW_MILLIS, easing = FastOutSlowInEasing))
    }

    val currentColor = MaterialTheme.colorScheme.primary
    val previousColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PREVIOUS_LINE_ALPHA)
    val gridColor = MaterialTheme.extendedColors.chartGrid
    val guideColor = MaterialTheme.colorScheme.outline
    val dotRingColor = MaterialTheme.colorScheme.surfaceContainer
    val labelBackdrop = labelBackdropColor()

    val currentTotal = pace.current.lastOrNull()?.total ?: Money.ZERO
    val previousTotal = pace.previous.lastOrNull()?.total ?: Money.ZERO

    // Text is laid out once per period rather than on every animation frame
    val dateFormatter = LocalDateFormatter.current
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.extendedColors.chartAxisText)
    val axisMax = remember(currentTotal, previousTotal) { niceAxisMax(maxOf(currentTotal, previousTotal)) }
    val gridLabels = remember(axisMax, axisStyle) {
        GridlineFractions.map { fraction ->
            fraction to textMeasurer.measure(currencyFormatter.formatShort(axisMax.atFraction(fraction)), axisStyle)
        }
    }
    val xLabels = remember(periodKey, axisStyle) {
        paceAxisLabels(pace, dateFormatter).map { it.index to textMeasurer.measure(it.text, axisStyle) }
    }
    val endLabel = remember(currentTotal, axisStyle, currentColor) {
        textMeasurer.measure(
            currencyFormatter.formatShort(currentTotal),
            axisStyle.copy(color = currentColor, fontWeight = FontWeight.SemiBold)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics { contentDescription = description }
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
            val hairline = 1.dp.toPx()
            val labelSpacing = LabelSpacing.toPx()
            val dotRadius = DotRadius.toPx()
            val lineWidth = LineWidth.toPx()
            // Inset so a dot on the first or last day isn't cut off by the edge
            val plotWidth = size.width - 2 * dotRadius

            fun xOf(index: Int): Float = dotRadius + plotWidth * index / (dayCount - 1)
            fun yOf(total: Money): Float = plotBottom - total.ratioTo(axisMax) * plotHeight
            fun pointOf(index: Int, point: PacePoint) = Offset(xOf(index), yOf(point.total))
            fun lineThrough(points: List<PacePoint>) = Path().apply {
                points.forEachIndexed { index, point ->
                    val offset = pointOf(index, point)
                    if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                }
            }

            drawLine(gridColor, Offset(0f, plotBottom), Offset(size.width, plotBottom), hairline)
            gridLabels.forEach { (fraction, _) ->
                val y = plotBottom - plotHeight * fraction
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), hairline)
            }

            if (pace.previous.size >= 2) {
                val dash = DashLength.toPx()
                drawPath(
                    path = lineThrough(pace.previous),
                    color = previousColor,
                    style = Stroke(
                        width = lineWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
                    )
                )
            }

            val lastIndex = pace.current.lastIndex
            if (lastIndex >= 0) {
                val end = pointOf(lastIndex, pace.current[lastIndex])

                if (lastIndex >= 1) {
                    val line = lineThrough(pace.current)
                    val area = lineThrough(pace.current).apply {
                        lineTo(end.x, plotBottom)
                        lineTo(xOf(0), plotBottom)
                        close()
                    }
                    // The line is drawn in by uncovering it from the left
                    clipRect(right = (end.x + lineWidth) * reveal.value) {
                        drawPath(
                            path = area,
                            brush = Brush.verticalGradient(
                                colors = listOf(currentColor.copy(alpha = AREA_ALPHA), Color.Transparent),
                                startY = plotTop,
                                endY = plotBottom
                            )
                        )
                        drawPath(
                            path = line,
                            color = currentColor,
                            style = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                drawCircle(dotRingColor, radius = dotRadius + lineWidth, center = end, alpha = reveal.value)
                drawCircle(currentColor, radius = dotRadius, center = end, alpha = reveal.value)
                val maxLeft = max(0f, size.width - endLabel.size.width)
                drawLabel(
                    label = endLabel,
                    topLeft = Offset(
                        x = (end.x - endLabel.size.width / 2f).coerceIn(0f, maxLeft),
                        y = end.y - endLabel.size.height - dotRadius - 2 * labelSpacing
                    ),
                    backdrop = labelBackdrop,
                    alpha = reveal.value
                )
            }

            selectedIndex?.let { index ->
                val x = xOf(index)
                drawLine(guideColor, Offset(x, plotTop), Offset(x, plotBottom), hairline)
                pace.previous.getOrNull(index)?.let {
                    drawCircle(guideColor, radius = dotRadius, center = pointOf(index, it))
                }
                pace.current.getOrNull(index)?.let {
                    drawCircle(dotRingColor, radius = dotRadius + lineWidth, center = pointOf(index, it))
                    drawCircle(currentColor, radius = dotRadius, center = pointOf(index, it))
                }
            }

            // Drawn last so they stay readable over the lines
            gridLabels.forEach { (fraction, label) ->
                val y = plotBottom - plotHeight * fraction
                drawLabel(
                    label = label,
                    topLeft = Offset(0f, y - label.size.height - labelSpacing),
                    backdrop = labelBackdrop
                )
            }

            xLabels.forEach { (index, label) ->
                val maxLeft = max(0f, size.width - label.size.width)
                drawLabel(
                    label = label,
                    topLeft = Offset(
                        x = (xOf(index) - label.size.width / 2f).coerceIn(0f, maxLeft),
                        y = plotBottom + labelSpacing * 2
                    ),
                    backdrop = Color.Transparent
                )
            }
        }

        selectedIndex?.let { index ->
            val text = paceTooltip(pace, index)
            if (text != null) {
                ChartTooltip(
                    text = text,
                    modifier = Modifier.centeredOn(index / (dayCount - 1f))
                )
            }
        }
    }
}

/** What each period had spent by the day at [index], e.g. `9 Oct ₹12,340 · 9 Sep ₹14,880`. */
@Composable
private fun paceTooltip(pace: SpendingPace, index: Int): String? {
    val current = pace.current.getOrNull(index)?.let { paceTooltipValue(it) }
    val previous = pace.previous.getOrNull(index)?.let { paceTooltipValue(it) }

    return when {
        current != null && previous != null -> stringResource(R.string.pace_tooltip_both, current, previous)
        else -> current ?: previous
    }
}

@Composable
private fun paceTooltipValue(point: PacePoint): String = stringResource(
    R.string.pace_tooltip_value,
    LocalDateFormatter.current.formatDayMonth(point.dateMillis),
    LocalCurrencyFormatter.current.formatWhole(point.total)
)

/** Every weekday for a week (`Mon` … `Sun`); every seventh day of the month beyond that. */
private fun paceAxisLabels(pace: SpendingPace, dateFormatter: DateFormatter): List<AxisLabel> =
    if (pace.dayCount <= DAYS_PER_LABEL) {
        // The previous period is always complete, so it has a date for every weekday
        pace.previous.mapIndexed { index, point -> AxisLabel(index, dateFormatter.formatWeekday(point.dateMillis)) }
    } else {
        // A month starts on the 1st, so the day of the month is the position plus one
        (0 until pace.dayCount step DAYS_PER_LABEL).map { AxisLabel(it, (it + 1).toString()) }
    }

@PreviewLightDark
@Composable
private fun SpendingPaceCardOngoingPreview() {
    ExpenseTrackerProTheme {
        SpendingPaceCard(pace = PreviewData.ongoingPace, isLoading = false)
    }
}

@PreviewLightDark
@Composable
private fun SpendingPaceCardFinishedPreview() {
    ExpenseTrackerProTheme {
        SpendingPaceCard(pace = PreviewData.finishedPace, isLoading = false)
    }
}

@PreviewLightDark
@Composable
private fun SpendingPaceCardLoadingPreview() {
    ExpenseTrackerProTheme {
        SpendingPaceCard(pace = null, isLoading = true)
    }
}

@PreviewLightDark
@Composable
private fun SpendingPaceCardEmptyPreview() {
    ExpenseTrackerProTheme {
        SpendingPaceCard(pace = null, isLoading = false)
    }
}
