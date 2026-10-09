package com.example.expensetrackerpro.presentation.transactions.transactionList.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.TransactionTypeCounts
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import com.example.expensetrackerpro.presentation.navigation.CategoryFilter
import com.example.expensetrackerpro.ui.theme.extendedColors

/** All / Expense / Income with how many each lists, then the category a chart narrowed the list to. */
@Composable
fun TransactionTypeChips(
    selected: TransactionTypeFilter,
    counts: TransactionTypeCounts,
    categoryFilter: CategoryFilter?,
    onSelect: (TransactionTypeFilter) -> Unit,
    onClearCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TypeChip(
            label = stringResource(R.string.transactions_filter_all),
            count = counts.all,
            accent = MaterialTheme.colorScheme.primary,
            isSelected = selected == TransactionTypeFilter.ALL,
            onClick = { onSelect(TransactionTypeFilter.ALL) }
        )
        TypeChip(
            label = stringResource(R.string.transaction_type_expense),
            count = counts.expense,
            accent = MaterialTheme.extendedColors.expense,
            isSelected = selected == TransactionTypeFilter.EXPENSE,
            onClick = { onSelect(TransactionTypeFilter.EXPENSE) }
        )
        TypeChip(
            label = stringResource(R.string.transaction_type_income),
            count = counts.income,
            accent = MaterialTheme.extendedColors.income,
            isSelected = selected == TransactionTypeFilter.INCOME,
            onClick = { onSelect(TransactionTypeFilter.INCOME) }
        )

        categoryFilter?.let { category ->
            InputChip(
                selected = true,
                onClick = onClearCategory,
                label = { Text(category.name) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.transactions_clear_category_filter),
                        modifier = Modifier.size(InputChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    count: Int,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(stringResource(R.string.transactions_filter_chip, label, count)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.copy(alpha = 0.16f),
            selectedLabelColor = accent
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            selectedBorderColor = accent,
            selectedBorderWidth = 1.dp
        )
    )
}
