package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftSource
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.ReviewPosition

private val BannerShape = RoundedCornerShape(16.dp)

/**
 * Says that the form was filled from a bill, or from the user's text, and should be checked, with
 * "Clear" to take those values out again. A bill shows a receipt, not the photo: the photo is
 * deleted once it is read. Text is quoted, so the user sees what was understood from what.
 */
@Composable
fun BillFilledBanner(
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    source: DraftSource = DraftSource.Bill,
    // Where the form is in a sentence's transactions being gone through one by one
    reviewPosition: ReviewPosition? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BannerShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (source) {
                    is DraftSource.Text -> Icons.Rounded.AutoAwesome
                    is DraftSource.History -> Icons.Rounded.History
                    DraftSource.Bill -> Icons.AutoMirrored.Filled.ReceiptLong
                },
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                val title = stringResource(
                    when (source) {
                        is DraftSource.Text -> R.string.quick_add_banner_title
                        is DraftSource.History -> R.string.quick_add_history_banner_title
                        DraftSource.Bill -> R.string.bill_fill_banner_title
                    }
                )
                Text(
                    text = reviewPosition
                        ?.let { stringResource(R.string.quick_add_review_banner_title, title, it.number, it.total) }
                        ?: title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (source) {
                        is DraftSource.Text -> stringResource(R.string.quick_add_banner_quote, source.text)
                        is DraftSource.History -> stringResource(R.string.quick_add_history_banner_message, source.title)
                        DraftSource.Bill -> stringResource(R.string.bill_fill_banner_message)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onClear) {
                Text(stringResource(R.string.bill_fill_clear))
            }
        }
    }
}

/** Shown under the banner when the bill's total could not be found. */
@Composable
fun BillTotalMissingNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = stringResource(R.string.bill_fill_total_missing),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@PreviewLightDark
@Composable
private fun BillFilledBannerPreview() {
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BillFilledBanner(onClear = {})
                BillTotalMissingNotice()
                BillFilledBanner(onClear = {}, source = DraftSource.Text("Spent 450 on swiggy yesterday, dinner with Rahul"))
            }
        }
    }
}
