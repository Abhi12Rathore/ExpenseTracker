package com.example.expensetrackerpro.presentation.transactions.transactionList.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.text.highlighted
import com.example.expensetrackerpro.ui.theme.extendedColors
import com.example.expensetrackerpro.utils.CategoryIcons
import com.example.expensetrackerpro.utils.CurrencyFormatter

private val CardCorner = 16.dp
private val IconTileShape = RoundedCornerShape(12.dp)

/** Where a row sits in its day's card, which decides the corners it rounds. */
enum class RowPosition(val isFirst: Boolean, val isLast: Boolean) {
    ONLY(isFirst = true, isLast = true),
    FIRST(isFirst = true, isLast = false),
    MIDDLE(isFirst = false, isLast = false),
    LAST(isFirst = false, isLast = true);

    companion object {
        fun of(index: Int, count: Int): RowPosition = when {
            count == 1 -> ONLY
            index == 0 -> FIRST
            index == count - 1 -> LAST
            else -> MIDDLE
        }
    }
}

/**
 * One transaction inside a day's card. Tapping opens it; swiping left deletes it. Occurrences
 * of [searchQuery] in the title and the line under it are marked out.
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    position: RowPosition,
    searchQuery: String,
    swipeResetToken: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    // Only decides whether the swipe may finish: it can be asked more than once for one swipe
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value -> value == SwipeToDismissBoxValue.EndToStart }
    )

    // True once the row has been seen at rest. A row that enters already swiped aside is an old
    // swipe restored with it (it came back through Undo), which must not delete it again.
    var isArmed by remember { mutableStateOf(false) }

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.Settled -> isArmed = true
            SwipeToDismissBoxValue.EndToStart ->
                if (isArmed) {
                    isArmed = false
                    currentOnDelete()
                } else {
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            SwipeToDismissBoxValue.StartToEnd -> Unit
        }
    }

    // A row stays swiped aside until the list drops it. One still here after a failed delete
    // slides back instead.
    LaunchedEffect(swipeResetToken) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) dismissState.reset()
    }

    val shape = RoundedCornerShape(
        topStart = if (position.isFirst) CardCorner else 0.dp,
        topEnd = if (position.isFirst) CardCorner else 0.dp,
        bottomStart = if (position.isLast) CardCorner else 0.dp,
        bottomEnd = if (position.isLast) CardCorner else 0.dp
    )

    val deleteLabel = stringResource(R.string.transaction_delete)

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .clip(shape)
            // Swiping isn't available to everyone, so screen readers get Delete as an action
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(deleteLabel) {
                        currentOnDelete()
                        true
                    }
                )
            },
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteBackground() }
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onClick)
        ) {
            RowContent(transaction = transaction, searchQuery = searchQuery)
            if (!position.isLast) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun RowContent(transaction: Transaction, searchQuery: String) {
    val isIncome = transaction.transactionType == TransactionType.INCOME
    val categoryColor = CategoryIcons.color(transaction.categoryName) ?: MaterialTheme.colorScheme.primary
    val meta = if (transaction.note.isEmpty()) {
        transaction.categoryName
    } else {
        stringResource(
            R.string.transactions_row_meta_with_note,
            transaction.categoryName,
            // Line breaks would cut the one-line preview short
            transaction.note.replace('\n', ' ')
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(categoryColor.copy(alpha = 0.15f), IconTileShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIcons.icons[transaction.categoryName] ?: Icons.Default.Category,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlighted(transaction.title, searchQuery),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = highlighted(meta, searchQuery),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = CurrencyFormatter.formatSigned(transaction.amount, isIncome),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) MaterialTheme.extendedColors.income else MaterialTheme.extendedColors.expense,
            maxLines = 1
        )
    }
}

@Composable
private fun DeleteBackground() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onError
        )
        Text(
            text = stringResource(R.string.transaction_delete),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onError
        )
    }
}
