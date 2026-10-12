package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R

private const val FILLED_TINT_ALPHA = 0.35f

/** The small "✨ From bill" pill beside a field a scanned bill filled in. */
@Composable
fun FromBillTag(modifier: Modifier = Modifier, text: String = stringResource(R.string.bill_fill_tag)) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** A section's label, followed by [tag] while a scanned bill's value is in the section. */
@Composable
fun TaggedSectionLabel(text: String, tag: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel(text = text)
        if (tag != null) FromBillTag(text = tag)
    }
}

/** A text field's usual colors, with a light tint while it holds a scanned bill's value. */
@Composable
fun billFieldColors(isFromBill: Boolean): TextFieldColors {
    if (!isFromBill) return OutlinedTextFieldDefaults.colors()

    val tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = FILLED_TINT_ALPHA)
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = tint,
        unfocusedContainerColor = tint,
        unfocusedBorderColor = MaterialTheme.colorScheme.primary
    )
}

@PreviewLightDark
@Composable
private fun TaggedSectionLabelPreview() {
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TaggedSectionLabel(
                text = stringResource(R.string.transaction_date),
                tag = stringResource(R.string.bill_fill_tag),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
