package com.example.expensetrackerpro.presentation.currency.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CurrencyPreview
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.presentation.common.motion.LocalAnimationsEnabled
import com.example.expensetrackerpro.utils.nameRes

// How long one currency's preview takes to fade into the next
private const val CROSS_FADE_MILLIS = 200

// Chart-axis samples: show k / L / Cr against K / M / B
private val AXIS_SAMPLE_SMALL = Money.ofRupees(12_000)
private val AXIS_SAMPLE_LARGE = Money.ofRupees(1_500_000)

/**
 * The user's own figures for this month, written in [currency], so a currency can be judged
 * before amounts are shown in it. A new currency fades in over the old one inside the same
 * frame, which never changes size, so nothing around it moves. [compact] keeps the balance and
 * its row only, for short screens.
 */
@Composable
fun CurrencyPreviewCard(
    preview: CurrencyPreview,
    currency: AppCurrency,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val animationsEnabled = LocalAnimationsEnabled.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        AnimatedContent(
            targetState = currency,
            transitionSpec = {
                if (animationsEnabled) {
                    fadeIn(tween(CROSS_FADE_MILLIS)) togetherWith fadeOut(tween(CROSS_FADE_MILLIS)) using null
                } else {
                    EnterTransition.None togetherWith ExitTransition.None using null
                }
            },
            label = "currencyPreview"
        ) { shown ->
            PreviewFrame(preview = preview, currency = shown, compact = compact)
        }
    }
}

@Composable
private fun PreviewFrame(preview: CurrencyPreview, currency: AppCurrency, compact: Boolean) {
    val currencyFormatter = LocalCurrencyFormatter.current

    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.currency_preview_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(currency.nameRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SummaryPreview(summary = preview.summary, currency = currency)
        if (!compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                preview.topCategory?.let { category ->
                    SampleTile(
                        label = category.categoryName,
                        value = currencyFormatter.format(-category.amount, currency),
                        icon = { CategoryIconTile(icon = category.icon, size = 28.dp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                SampleTile(
                    label = stringResource(R.string.currency_preview_chart_axis),
                    value = stringResource(
                        R.string.currency_preview_axis_values,
                        currencyFormatter.formatShort(AXIS_SAMPLE_SMALL, currency),
                        currencyFormatter.formatShort(AXIS_SAMPLE_LARGE, currency)
                    ),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryPreview(summary: FinancialSummary, currency: AppCurrency) {
    val currencyFormatter = LocalCurrencyFormatter.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.currency_preview_balance),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currencyFormatter.format(summary.totalBalance, currency),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryValue(
                    label = stringResource(R.string.summary_income),
                    value = currencyFormatter.formatWhole(summary.totalIncome, currency),
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = stringResource(R.string.summary_expense),
                    value = currencyFormatter.formatWhole(summary.totalExpense, currency),
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = stringResource(R.string.summary_savings),
                    value = stringResource(R.string.percent_value, summary.savingsRate.toInt())
                )
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SampleTile(
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val previewData = CurrencyPreview(
    summary = FinancialSummary(
        totalBalance = Money.ofRupees(50_422),
        totalIncome = Money.ofRupees(105_500),
        totalExpense = Money.ofRupees(55_078),
        savingsRate = 47f
    ),
    topCategory = CategoryBreakdown(
        categoryId = CategoryId(1),
        categoryName = "Food & Dining",
        icon = CategoryIcon.RESTAURANT,
        amount = Money.ofRupees(1_130)
    )
)

@PreviewLightDark
@Composable
private fun CurrencyPreviewCardPreview() {
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CurrencyPreviewCard(preview = previewData, currency = AppCurrency.USD, modifier = Modifier.padding(16.dp))
        }
    }
}
