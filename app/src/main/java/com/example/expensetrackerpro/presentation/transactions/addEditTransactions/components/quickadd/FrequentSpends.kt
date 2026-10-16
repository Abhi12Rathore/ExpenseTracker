package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.quickadd

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.FrequentTransaction
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.SectionLabel

private val CardShape = RoundedCornerShape(14.dp)
private val CardMaxWidth = 180.dp

/**
 * The user's most frequent transactions, a card each, to repeat one in a tap: its title and
 * category, its usual amount and how often it came up. Takes no room when there are none.
 */
@Composable
fun FrequentSpends(
    frequent: List<FrequentTransaction>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (frequent.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(text = stringResource(R.string.quick_add_frequent))
        // Three cards may not fit a narrow phone in a long language, so they scroll
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            frequent.forEachIndexed { index, item ->
                FrequentCard(item, onClick = { onSelected(index) })
            }
        }
    }
}

@Composable
private fun FrequentCard(item: FrequentTransaction, onClick: () -> Unit) {
    val amount = LocalCurrencyFormatter.current.format(item.usualAmount)
    val times = pluralStringResource(R.plurals.quick_add_frequent_times, item.count, item.count)

    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(max = CardMaxWidth),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconTile(icon = item.categoryIcon, size = 32.dp)
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.quick_add_frequent_detail, amount, times),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FrequentSpendsPreview() {
    fun frequent(title: String, icon: CategoryIcon, rupees: Long, count: Int) = FrequentTransaction(
        title = title,
        categoryId = CategoryId(1),
        categoryName = "",
        categoryIcon = icon,
        type = TransactionType.EXPENSE,
        usualAmount = Money.ofRupees(rupees),
        count = count
    )
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FrequentSpends(
                frequent = listOf(
                    frequent("Swiggy", CategoryIcon.RESTAURANT, 450, 6),
                    frequent("Petrol", CategoryIcon.LOCAL_GAS_STATION, 2000, 4),
                    frequent("Rent", CategoryIcon.HOME, 18000, 3)
                ),
                onSelected = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
