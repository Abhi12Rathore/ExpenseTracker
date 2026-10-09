package com.example.expensetrackerpro.presentation.homeScreen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.homeScreen.component.charts.BalanceSparkline
import com.example.expensetrackerpro.utils.CurrencyFormatter
import com.example.expensetrackerpro.utils.DateFormatter
import kotlin.math.abs

private const val CHIP_BACKGROUND_ALPHA = 0.08f
private const val MIN_AMOUNT_FONT_SCALE = 0.6f

@Composable
fun FinancialSummaryCard(
    data: FinancialSummary,
    periodLabel: String,
    comparison: PeriodComparison? = null,
    balanceTrend: List<Double> = emptyList()
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.balance_label, periodLabel), style = MaterialTheme.typography.labelMedium)
                    ShrinkToFitText(
                        text = CurrencyFormatter.format(data.totalBalance),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black)
                    )
                }
                // A balance that never moved has no trend to draw
                if (balanceTrend.distinct().size >= 2) {
                    Spacer(Modifier.width(12.dp))
                    BalanceSparkline(
                        values = balanceTrend,
                        modifier = Modifier.size(width = 72.dp, height = 36.dp)
                    )
                }
            }

            comparison?.let {
                Spacer(modifier = Modifier.height(8.dp))
                ComparisonChip(it)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Income", CurrencyFormatter.format(data.totalIncome), Icons.Rounded.TrendingUp)
                SummaryItem("Expense", CurrencyFormatter.format(data.totalExpense), Icons.Rounded.TrendingDown)
                SummaryItem("Savings", "${data.savingsRate.toInt()}%", Icons.Rounded.AutoGraph)
            }
        }
    }
}

/** e.g. `▲ ₹3,200 more saved than Sep`, against the period before the selected one. */
@Composable
private fun ComparisonChip(comparison: PeriodComparison) {
    val previousPeriod = when (comparison.granularity) {
        PeriodGranularity.WEEK -> stringResource(R.string.comparison_last_week)
        PeriodGranularity.MONTH -> DateFormatter.formatShortMonth(comparison.previousRange.startMillis)
        PeriodGranularity.YEAR -> stringResource(R.string.comparison_last_year)
    }
    val difference = comparison.balanceDifference
    val amount = CurrencyFormatter.formatWhole(abs(difference))
    val text = when {
        // Less than a rupee apart reads as no difference
        abs(difference) < 0.5 -> stringResource(R.string.comparison_same_saved, previousPeriod)
        difference > 0 -> stringResource(R.string.comparison_more_saved, amount, previousPeriod)
        else -> stringResource(R.string.comparison_less_saved, amount, previousPeriod)
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = CHIP_BACKGROUND_ALPHA),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** One line of text that steps its font size down until it fits the width it is given. */
@Composable
private fun ShrinkToFitText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    var fontScale by remember(text) { mutableFloatStateOf(1f) }

    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = style.fontSize * fontScale),
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontScale > MIN_AMOUNT_FONT_SCALE) fontScale *= 0.9f
        }
    )
}

@Composable
private fun SummaryItem(label: String, value: String, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null,
                Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@PreviewLightDark
@Composable
private fun FinancialSummaryCardPreview() {
    ExpenseTrackerProTheme {
        FinancialSummaryCard(
            data = PreviewData.financialSummary,
            periodLabel = DateFormatter.formatShortMonthYear(PreviewData.DATE_MILLIS),
            comparison = PreviewData.periodComparison,
            balanceTrend = PreviewData.balanceTrend
        )
    }
}
