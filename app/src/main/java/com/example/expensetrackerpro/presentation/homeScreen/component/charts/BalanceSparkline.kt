package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.presentation.common.preview.PreviewData

private val LineWidth = 2.dp
private val EndDotRadius = 3.dp
private const val FILL_ALPHA = 0.25f

/** A small unlabelled line of [values], oldest first, scaled to fill its own height. */
@Composable
fun BalanceSparkline(
    values: List<Money>,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val dotRadius = EndDotRadius.toPx()
        val lowest = values.min()
        val span = values.max() - lowest
        val stepX = (size.width - 2 * dotRadius) / (values.size - 1)
        val drawableHeight = size.height - 2 * dotRadius

        val points = values.mapIndexed { index, value ->
            // A flat trend has no span to scale by, so it runs through the middle
            val height = if (span.isPositive) (value - lowest).ratioTo(span) else 0.5f
            Offset(
                x = dotRadius + index * stepX,
                y = dotRadius + drawableHeight * (1f - height)
            )
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val fill = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(listOf(color.copy(alpha = FILL_ALPHA), Color.Transparent))
        )
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(color = color, radius = dotRadius, center = points.last())
    }
}

@PreviewLightDark
@Composable
private fun BalanceSparklinePreview() {
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            BalanceSparkline(
                values = PreviewData.balanceTrend,
                modifier = Modifier
                    .padding(16.dp)
                    .size(width = 72.dp, height = 36.dp)
            )
        }
    }
}
