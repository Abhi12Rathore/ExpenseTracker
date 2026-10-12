package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.utils.CurrencyFormatter

/** The amounts found on the bill, to put the right one into the form in one tap. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillAmountChips(
    amounts: List<Money>,
    selected: Money?,
    onAmountSelected: (Money) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(text = stringResource(R.string.bill_fill_other_amounts))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            amounts.forEach { amount ->
                FilterChip(
                    selected = amount == selected,
                    onClick = { onAmountSelected(amount) },
                    label = { Text(CurrencyFormatter.format(amount)) }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BillAmountChipsPreview() {
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BillAmountChips(
                amounts = listOf(Money.ofRupees(1428), Money.ofRupees(1360), Money.ofRupees(690)),
                selected = Money.ofRupees(1428),
                onAmountSelected = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
