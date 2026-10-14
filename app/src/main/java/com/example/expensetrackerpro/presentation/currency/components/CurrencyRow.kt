package com.example.expensetrackerpro.presentation.currency.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.presentation.language.components.ScriptBadge
import com.example.expensetrackerpro.utils.nameRes

private const val SELECTED_ROW_ALPHA = 0.08f

// Written in each row's own grouping, so lakh against thousand shows before choosing
private val SAMPLE_AMOUNT = Money.ofRupees(100_000)

/**
 * One choice on the Currency screen: its sign, its name and code, and a sample amount written
 * as it would be in that currency.
 */
@Composable
fun CurrencyRow(
    currency: AppCurrency,
    isCurrent: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_ROW_ALPHA) else Color.Transparent
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScriptBadge(text = currency.badge, selected = selected)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(currency.nameRes()),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isCurrent) {
                    stringResource(R.string.currency_row_current, currency.code)
                } else {
                    currency.code
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = LocalCurrencyFormatter.current.formatWhole(SAMPLE_AMOUNT, currency),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                // The row already reports itself as selected
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CurrencyRowPreview() {
    ExpenseTrackerProTheme {
        Surface {
            Column {
                CurrencyRow(currency = AppCurrency.INR, isCurrent = true, selected = false, onClick = {})
                CurrencyRow(currency = AppCurrency.USD, isCurrent = false, selected = true, onClick = {})
                CurrencyRow(currency = AppCurrency.AED, isCurrent = false, selected = false, onClick = {})
            }
        }
    }
}
