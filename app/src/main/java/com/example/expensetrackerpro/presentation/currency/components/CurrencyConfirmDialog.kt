package com.example.expensetrackerpro.presentation.currency.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.CurrencyChangeExample
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.presentation.common.haptics.rememberHaptics
import com.example.expensetrackerpro.utils.nameRes

/**
 * Says what changing the currency does, with the user's latest transaction as the example:
 * the same number with another symbol. Nothing is converted.
 */
@Composable
fun CurrencyConfirmDialog(
    example: CurrencyChangeExample,
    from: AppCurrency,
    to: AppCurrency,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val currencyFormatter = LocalCurrencyFormatter.current
    val haptics = rememberHaptics()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_confirm_title, stringResource(to.nameRes()))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.currency_confirm_body,
                        example.transactionCount,
                        example.transactionCount
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currencyFormatter.format(example.latestAmount, from),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.currency_confirm_becomes),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currencyFormatter.format(example.latestAmount, to),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.currency_confirm_nothing_converted),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.currency_confirm_explanation),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptics.confirm()
                    onConfirm()
                }
            ) {
                Text(stringResource(R.string.currency_confirm_change))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.currency_confirm_cancel))
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun CurrencyConfirmDialogPreview() {
    ExpenseTrackerProTheme {
        CurrencyConfirmDialog(
            example = CurrencyChangeExample(
                transactionCount = 299,
                latestAmount = Money.ofRupees(450)
            ),
            from = AppCurrency.INR,
            to = AppCurrency.USD,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
