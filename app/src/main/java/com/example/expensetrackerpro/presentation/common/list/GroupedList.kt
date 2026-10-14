package com.example.expensetrackerpro.presentation.common.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val CardCorner = 20.dp

/** The title over a section of a picker list, such as "Suggested". */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .semantics { heading() }
    )
}

/**
 * One row of a section's card. Each row is its own list item, so the card is drawn in pieces:
 * the first rounds the top, the last the bottom, and every row after the first starts with a divider.
 */
@Composable
fun GroupedRow(index: Int, count: Int, content: @Composable () -> Unit) {
    val first = index == 0
    val last = index == count - 1
    val shape = RoundedCornerShape(
        topStart = if (first) CardCorner else 0.dp,
        topEnd = if (first) CardCorner else 0.dp,
        bottomStart = if (last) CardCorner else 0.dp,
        bottomEnd = if (last) CardCorner else 0.dp
    )

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (!first) RowDivider()
            content()
        }
    }
}

/** Starts under the text, past the badge. */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
