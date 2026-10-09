package com.example.expensetrackerpro.presentation.transactions.transactionList.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import com.example.expensetrackerpro.presentation.common.filter.shortLabel
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListUiState

/** Shown in place of the list: why it is empty and, for a search, the way to widen it. */
@Composable
fun TransactionsEmptyState(
    uiState: TransactionListUiState,
    onSearchAllTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isShowingSearchResults) {
        val query = uiState.searchQuery.trim()
        EmptyMessage(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.transactions_no_matches_title),
            description = if (uiState.isAllTime) {
                stringResource(R.string.transactions_no_matches_all_time_description, query)
            } else {
                stringResource(R.string.transactions_no_matches_description, query)
            },
            modifier = modifier
        ) {
            if (!uiState.isAllTime) {
                FilledTonalButton(onClick = onSearchAllTime) {
                    Text(stringResource(R.string.transactions_search_all_time))
                }
            }
        }
    } else {
        val titleRes = when (uiState.typeFilter) {
            TransactionTypeFilter.ALL -> R.string.transactions_empty_all
            TransactionTypeFilter.EXPENSE -> R.string.transactions_empty_expense
            TransactionTypeFilter.INCOME -> R.string.transactions_empty_income
        }
        EmptyMessage(
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            title = stringResource(titleRes, periodPhrase(uiState)),
            description = stringResource(R.string.transactions_empty_hint),
            modifier = modifier
        )
    }
}

/** The period as it ends "No income …": `this month`, `in Sep 2026` or `yet`. */
@Composable
private fun periodPhrase(uiState: TransactionListUiState): String {
    val filter = uiState.filter.filter

    return when {
        uiState.isAllTime -> stringResource(R.string.transactions_empty_all_time)
        // The period that can't be stepped forward is the one we are in
        filter is DateFilter.Period && !uiState.filter.canStepForward -> stringResource(
            when (filter.granularity) {
                PeriodGranularity.WEEK -> R.string.transactions_empty_this_week
                PeriodGranularity.MONTH -> R.string.transactions_empty_this_month
                PeriodGranularity.YEAR -> R.string.transactions_empty_this_year
            }
        )
        else -> stringResource(R.string.transactions_empty_in_period, uiState.filter.shortLabel())
    }
}

@Composable
private fun EmptyMessage(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        action()
    }
}
