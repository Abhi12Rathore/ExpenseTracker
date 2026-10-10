package com.example.expensetrackerpro.presentation.manageCategories.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.utils.CurrencyFormatter

/** A category with how much it is used. Tapping anywhere on it opens it for editing. */
@Composable
fun CategoryManageTile(
    usage: CategoryUsage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = usage.category

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryIconTile(icon = category.icon)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // Gives way to the badge, which is never cut
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!category.isCustom) DefaultBadge()
                }
                Text(
                    text = usageText(usage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DefaultBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = stringResource(R.string.manage_categories_default_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun usageText(usage: CategoryUsage): String = when {
    usage.periodCount > 0 -> pluralStringResource(
        R.plurals.manage_categories_usage_this_month,
        usage.periodCount,
        usage.periodCount,
        CurrencyFormatter.formatWhole(usage.periodAmount)
    )
    usage.totalCount > 0 -> pluralStringResource(
        R.plurals.manage_categories_usage_total,
        usage.totalCount,
        usage.totalCount
    )
    else -> stringResource(R.string.manage_categories_usage_none)
}

@PreviewLightDark
@Composable
private fun CategoryManageTilePreview() {
    ExpenseTrackerProTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewData.categoryUsages.forEach { CategoryManageTile(usage = it, onClick = {}) }
            }
        }
    }
}
