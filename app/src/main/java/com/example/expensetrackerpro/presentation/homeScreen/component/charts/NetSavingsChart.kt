package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.MonthlyNet
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.ui.theme.extendedColors
import com.example.expensetrackerpro.utils.CurrencyFormatter
import com.example.expensetrackerpro.presentation.common.date.LocalDateFormatter
import kotlin.math.max

private val ChartHeight = 200.dp
private val TopPadding = 8.dp
private val AxisLabelHeight = 20.dp
private val BarGap = 4.dp
private val MinBarHeight = 2.dp

/** Income minus expense per month of a year, with loading and empty states. */
@Composable
fun NetSavingsCard(
    months: List<MonthlyNet>,
    isLoading: Boolean,
    onMonthClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    ChartCard(title = stringResource(R.string.net_savings_title), modifier = modifier) {
        when {
            isLoading -> SpendingBarsShimmer()
            months.none { !it.net.isZero } -> ChartEmptyMessage(stringResource(R.string.net_savings_empty))
            else -> {
                NetSavingsChart(months = months, onMonthClick = onMonthClick)
                ChartLegend {
                    ChartLegendItem(
                        color = MaterialTheme.extendedColors.income,
                        label = stringResource(R.string.net_savings_legend_saved)
                    )
                    ChartLegendItem(
                        color = MaterialTheme.extendedColors.expense,
                        label = stringResource(R.string.net_savings_legend_overspent)
                    )
                }
            }
        }
    }
}

/**
 * One bar per month from a shared baseline: up when more came in than went out, down when the
 * month was overspent. Only the worst month carries its amount. Tapping a month that has
 * started calls [onMonthClick] with the start of that month.
 */
@Composable
fun NetSavingsChart(
    months: List<MonthlyNet>,
    onMonthClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (months.isEmpty()) return

    // Names the year on show, not its amounts: saving a transaction must not replay the animation
    val periodKey = months.first().range.startMillis to months.size
    val currentMonths by rememberUpdatedState(months)
    val currentOnMonthClick by rememberUpdatedState(onMonthClick)

    val startsDrawn = chartStartsDrawn()
    val growth = remember(periodKey) { Animatable(if (startsDrawn) 1f else 0f) }
    LaunchedEffect(periodKey) {
        growth.animateTo(1f, tween(STAGGERED_GROW_MILLIS, easing = LinearEasing))
    }

    val savedColor = MaterialTheme.extendedColors.income
    val overspentColor = MaterialTheme.extendedColors.expense
    val futureBarColor = futureBarColor()
    val baselineColor = MaterialTheme.colorScheme.outlineVariant

    // How far the bars reach above and below the baseline
    val highest = maxOf(Money.ZERO, months.maxOf { it.net })
    val lowest = maxOf(Money.ZERO, -months.minOf { it.net })
    val worstIndex = months.indices.filter { months[it].net.isNegative }.minByOrNull { months[it].net }

    val dateFormatter = LocalDateFormatter.current
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.extendedColors.chartAxisText)
    val xLabels = remember(periodKey, axisStyle) {
        months.map { textMeasurer.measure(dateFormatter.formatMonthInitial(it.range.startMillis), axisStyle) }
    }
    val worstLabel = remember(worstIndex, months, axisStyle, overspentColor) {
        worstIndex?.let {
            textMeasurer.measure(
                CurrencyFormatter.formatShort(months[it].net),
                axisStyle.copy(color = overspentColor, fontWeight = FontWeight.SemiBold)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .pointerInput(periodKey) {
                detectTapGestures { offset ->
                    val index = (offset.x / size.width * currentMonths.size).toInt()
                    currentMonths.getOrNull(index)
                        ?.takeIf { !it.isFuture }
                        ?.let { currentOnMonthClick(it.range.startMillis) }
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val labelSpacing = LabelSpacing.toPx()
            val plotTop = TopPadding.toPx()
            val axisTop = size.height - AxisLabelHeight.toPx()
            // Room under the deepest bar for its amount
            val worstLabelSpace = worstLabel?.let { it.size.height + 2 * labelSpacing } ?: 0f
            val plotHeight = axisTop - plotTop - worstLabelSpace
            val span = highest + lowest
            val baseline = plotTop + highest.ratioTo(span) * plotHeight

            val slotWidth = size.width / months.size
            val gap = BarGap.toPx()
            val barWidth = (slotWidth - gap).coerceAtLeast(1f)

            months.forEachIndexed { index, month ->
                val left = index * slotWidth + gap / 2
                when {
                    month.isFuture ->
                        drawBar(left, baseline, barWidth, FutureBarHeight.toPx(), futureBarColor)
                    !month.net.isZero -> {
                        val fullHeight = max(month.net.abs().ratioTo(span) * plotHeight, MinBarHeight.toPx())
                        val height = fullHeight * barGrowth(growth.value, index, months.size)
                        val saved = month.net.isPositive
                        drawBar(
                            left = left,
                            baseline = baseline,
                            width = barWidth,
                            height = height,
                            color = if (saved) savedColor else overspentColor,
                            upward = saved
                        )

                        if (index == worstIndex && worstLabel != null) {
                            val maxLeft = max(0f, size.width - worstLabel.size.width)
                            drawLabel(
                                label = worstLabel,
                                topLeft = Offset(
                                    x = (left + barWidth / 2f - worstLabel.size.width / 2f).coerceIn(0f, maxLeft),
                                    y = baseline + height + labelSpacing
                                ),
                                backdrop = Color.Transparent,
                                alpha = growth.value
                            )
                        }
                    }
                }
            }

            // Over the bars, so the line they stand on stays whole
            drawLine(baselineColor, Offset(0f, baseline), Offset(size.width, baseline), 1.dp.toPx())

            xLabels.forEachIndexed { index, label ->
                drawLabel(
                    label = label,
                    topLeft = Offset(
                        x = (index + 0.5f) * slotWidth - label.size.width / 2f,
                        y = axisTop + labelSpacing * 2
                    ),
                    backdrop = Color.Transparent
                )
            }
        }

        // The canvas has no children, so each bar gets an invisible node for screen readers
        Row(modifier = Modifier.matchParentSize()) {
            months.forEach { month ->
                val description = stringResource(
                    if (month.net.isNegative) R.string.net_savings_bar_overspent else R.string.net_savings_bar_saved,
                    dateFormatter.formatMonthYear(month.range.startMillis),
                    CurrencyFormatter.format(month.net.abs())
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            contentDescription = description
                            if (!month.isFuture) {
                                onClick {
                                    onMonthClick(month.range.startMillis)
                                    true
                                }
                            }
                        }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NetSavingsCardPreview() {
    ExpenseTrackerProTheme {
        NetSavingsCard(months = PreviewData.netSavings, isLoading = false, onMonthClick = {})
    }
}

@PreviewLightDark
@Composable
private fun NetSavingsCardAllSavedPreview() {
    ExpenseTrackerProTheme {
        NetSavingsCard(
            months = PreviewData.netSavings.map { it.copy(net = it.net.abs()) },
            isLoading = false,
            onMonthClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun NetSavingsCardLoadingPreview() {
    ExpenseTrackerProTheme {
        NetSavingsCard(months = emptyList(), isLoading = true, onMonthClick = {})
    }
}

@PreviewLightDark
@Composable
private fun NetSavingsCardEmptyPreview() {
    ExpenseTrackerProTheme {
        NetSavingsCard(months = emptyList(), isLoading = false, onMonthClick = {})
    }
}
