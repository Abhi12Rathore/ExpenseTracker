package com.example.expensetrackerpro.presentation.transactions.transactionList.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.TransactionListSummary
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.ui.theme.extendedColors
import com.example.expensetrackerpro.utils.CurrencyFormatter

private data class SummaryCell(val label: String, val value: String, val color: Color)

/** The three figures for what the list is showing. Amounts are whole rupees to fit three across. */
@Composable
fun TransactionSummaryStrip(summary: TransactionListSummary, modifier: Modifier = Modifier) {
    val neutral = MaterialTheme.colorScheme.onSurface
    val income = MaterialTheme.extendedColors.income
    val expense = MaterialTheme.extendedColors.expense
    // A balance: green when money was left over, red when more went out than came in
    val netColor = { net: Double -> if (net < 0) expense else income }

    val cells = when (summary) {
        is TransactionListSummary.Overview -> listOf(
            SummaryCell(
                stringResource(R.string.transactions_summary_income),
                CurrencyFormatter.formatWhole(summary.income),
                income
            ),
            SummaryCell(
                stringResource(R.string.transactions_summary_expense),
                CurrencyFormatter.formatWhole(summary.expense),
                expense
            ),
            SummaryCell(
                stringResource(R.string.transactions_summary_net),
                CurrencyFormatter.formatWhole(summary.net),
                netColor(summary.net)
            )
        )
        is TransactionListSummary.SingleType -> listOf(
            SummaryCell(
                stringResource(R.string.transactions_summary_total),
                CurrencyFormatter.formatWhole(summary.total),
                if (summary.type == TransactionType.INCOME) income else expense
            ),
            SummaryCell(stringResource(R.string.transactions_summary_entries), summary.count.toString(), neutral),
            SummaryCell(
                stringResource(R.string.transactions_summary_biggest),
                CurrencyFormatter.formatWhole(summary.biggest),
                neutral
            )
        )
        is TransactionListSummary.Search -> listOf(
            SummaryCell(stringResource(R.string.transactions_summary_results), summary.count.toString(), neutral),
            SummaryCell(
                stringResource(R.string.transactions_summary_total),
                CurrencyFormatter.formatWhole(summary.total),
                netColor(summary.total)
            ),
            SummaryCell(
                stringResource(R.string.transactions_summary_average),
                CurrencyFormatter.formatWhole(summary.average),
                netColor(summary.average)
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cells.forEach { cell ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cell.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = cell.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cell.color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
