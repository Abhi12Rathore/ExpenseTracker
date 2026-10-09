package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R

private val ChartCardShape = RoundedCornerShape(20.dp)
private val PlaceholderShape = RoundedCornerShape(6.dp)

/** The container every dashboard chart sits in: a title, an optional subtitle and the chart. */
@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ChartCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

/** Shown in place of a chart that has nothing to draw. */
@Composable
fun ChartEmptyMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

/** A highlight that sweeps left to right, for placeholder shapes shown while data loads. */
@Composable
private fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val transition = rememberInfiniteTransition(label = "shimmer")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing)),
        label = "shimmerSweep"
    )

    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(sweep - 400f, 0f),
        end = Offset(sweep, 0f)
    )
}

// Bar heights as a share of the chart, so the placeholder reads as a bar chart
private val ShimmerBarHeights = listOf(0.35f, 0.6f, 0.45f, 0.8f, 0.3f, 0.55f, 0.7f, 0.4f, 0.65f, 0.5f, 0.75f, 0.45f)

@Composable
internal fun SpendingBarsShimmer(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ShimmerBarHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(height)
                    .background(brush, PlaceholderShape)
            )
        }
    }
}

/** One block the size of a chart, for charts whose shape a few bars wouldn't suggest. */
@Composable
internal fun ChartBlockShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(rememberShimmerBrush(), RoundedCornerShape(12.dp))
    )
}

@Composable
internal fun CategoryListShimmer(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(brush, RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(12.dp)
                            .background(brush, PlaceholderShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(brush, PlaceholderShape)
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ChartCardLoadingPreview() {
    ExpenseTrackerProTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ChartCard(title = stringResource(R.string.spending_daily_title)) { SpendingBarsShimmer() }
            ChartCard(title = stringResource(R.string.category_spending_title)) { CategoryListShimmer() }
        }
    }
}

@PreviewLightDark
@Composable
private fun ChartCardEmptyPreview() {
    ExpenseTrackerProTheme {
        ChartCard(title = stringResource(R.string.spending_daily_title)) {
            ChartEmptyMessage(stringResource(R.string.spending_empty))
        }
    }
}
