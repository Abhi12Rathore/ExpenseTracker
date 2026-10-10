package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryRanking
import com.example.expensetrackerpro.domain.models.CategoryShare
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.utils.CurrencyFormatter
import kotlin.math.roundToInt

private val RowShape = RoundedCornerShape(12.dp)
private val IconTileSize = 40.dp
private const val ICON_TINT_ALPHA = 0.12f
private const val SHARE_BAR_MILLIS = 600

/** Spending per category as a ranked list, with its loading and empty states. */
@Composable
fun CategorySpendingCard(
    ranking: CategoryRanking,
    isLoading: Boolean,
    onCategoryClick: (CategoryBreakdown) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasCategories = ranking.categoryCount > 0
    val subtitle = if (!isLoading && hasCategories) {
        pluralStringResource(
            R.plurals.category_spending_subtitle,
            ranking.categoryCount,
            CurrencyFormatter.formatWhole(ranking.total),
            ranking.categoryCount
        )
    } else {
        null
    }

    ChartCard(
        title = stringResource(R.string.category_spending_title),
        modifier = modifier,
        subtitle = subtitle
    ) {
        when {
            isLoading -> CategoryListShimmer()
            !hasCategories -> ChartEmptyMessage(stringResource(R.string.category_spending_empty))
            else -> CategoryRankedList(ranking = ranking, onCategoryClick = onCategoryClick)
        }
    }
}

/** The leading categories, then an "Other" row that expands into the rest. */
@Composable
fun CategoryRankedList(
    ranking: CategoryRanking,
    onCategoryClick: (CategoryBreakdown) -> Unit,
    modifier: Modifier = Modifier
) {
    var othersExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ranking.top.forEach { share ->
            CategoryRow(share = share, onClick = { onCategoryClick(share.category) })
        }

        if (ranking.others.isNotEmpty()) {
            RankedRow(
                leading = { OtherIconTile() },
                name = stringResource(R.string.category_spending_other),
                amount = ranking.othersAmount,
                fraction = ranking.othersFraction,
                onClick = { othersExpanded = !othersExpanded },
                onClickLabel = stringResource(
                    if (othersExpanded) {
                        R.string.category_spending_collapse_other
                    } else {
                        R.string.category_spending_expand_other
                    }
                ),
                trailingIcon = if (othersExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore
            )

            AnimatedVisibility(visible = othersExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ranking.others.forEach { share ->
                        CategoryRow(share = share, onClick = { onCategoryClick(share.category) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(share: CategoryShare, onClick: () -> Unit) {
    val category = share.category

    RankedRow(
        leading = { CategoryIconTile(icon = category.icon, size = IconTileSize) },
        name = category.categoryName,
        amount = category.amount,
        fraction = share.fraction,
        onClick = onClick
    )
}

/** Stands in for a category icon on the row that sums up the rest. */
@Composable
private fun OtherIconTile() {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(IconTileSize)
            .background(tint.copy(alpha = ICON_TINT_ALPHA), RowShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreHoriz,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun RankedRow(
    leading: @Composable () -> Unit,
    name: String,
    amount: Money,
    fraction: Float,
    onClick: () -> Unit,
    onClickLabel: String? = null,
    trailingIcon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .clickable(onClickLabel = onClickLabel, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = CurrencyFormatter.format(amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ShareBar(fraction = fraction, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = percentLabel(fraction),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        trailingIcon?.let {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A thin bar filled to [fraction], growing from empty when it first appears. */
@Composable
private fun ShareBar(fraction: Float, modifier: Modifier = Modifier) {
    val isPreview = LocalInspectionMode.current
    var appeared by remember { mutableStateOf(isPreview) }
    LaunchedEffect(Unit) { appeared = true }

    val filled by animateFloatAsState(
        targetValue = if (appeared) fraction.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(SHARE_BAR_MILLIS),
        label = "shareBar"
    )

    Box(
        modifier = modifier
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(filled)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun percentLabel(fraction: Float): String {
    val percent = (fraction * 100).roundToInt()

    return if (percent == 0 && fraction > 0f) {
        stringResource(R.string.percent_below_one)
    } else {
        stringResource(R.string.percent_value, percent)
    }
}

@PreviewLightDark
@Composable
private fun CategorySpendingCardPreview() {
    ExpenseTrackerProTheme {
        CategorySpendingCard(
            ranking = PreviewData.categoryRanking,
            isLoading = false,
            onCategoryClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun CategorySpendingCardLoadingPreview() {
    ExpenseTrackerProTheme {
        CategorySpendingCard(ranking = CategoryRanking(), isLoading = true, onCategoryClick = {})
    }
}

@PreviewLightDark
@Composable
private fun CategorySpendingCardEmptyPreview() {
    ExpenseTrackerProTheme {
        CategorySpendingCard(ranking = CategoryRanking(), isLoading = false, onCategoryClick = {})
    }
}
