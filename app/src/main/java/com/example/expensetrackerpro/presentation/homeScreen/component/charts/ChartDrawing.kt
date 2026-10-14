package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.expensetrackerpro.presentation.common.motion.LocalAnimationsEnabled
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal val BarCornerRadius = 4.dp
internal val FutureBarHeight = 6.dp
internal val LabelSpacing = 2.dp

private const val FUTURE_BAR_ALPHA = 0.1f
// Labels sit on a patch of the card colour so bars and lines behind them don't cut through
private const val LABEL_BACKDROP_ALPHA = 0.85f

private const val BAR_GROW_MILLIS = 400
// How much later the last bar starts growing than the first
private const val STAGGER_MILLIS = 300

/** How long a whole staggered bar animation runs. */
internal const val STAGGERED_GROW_MILLIS = BAR_GROW_MILLIS + STAGGER_MILLIS

/** The fill of a bar standing in for a day or month that hasn't started. */
@Composable
@ReadOnlyComposable
internal fun futureBarColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = FUTURE_BAR_ALPHA)

@Composable
@ReadOnlyComposable
internal fun labelBackdropColor(): Color =
    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = LABEL_BACKDROP_ALPHA)

/** A chart is drawn in full at once in a preview and with animations off; otherwise it grows in. */
@Composable
@ReadOnlyComposable
internal fun chartStartsDrawn(): Boolean = LocalInspectionMode.current || !LocalAnimationsEnabled.current

/** How far the bar at [index] has grown (0 to 1) at [progress] of the whole staggered animation. */
internal fun barGrowth(progress: Float, index: Int, count: Int): Float {
    val delay = if (count > 1) STAGGER_MILLIS * index / (count - 1f) else 0f
    val elapsed = progress * STAGGERED_GROW_MILLIS - delay

    return FastOutSlowInEasing.transform((elapsed / BAR_GROW_MILLIS).coerceIn(0f, 1f))
}

/**
 * A bar growing [height] away from [baseline], up by default or down when not [upward]. Only
 * the corners at its far end are rounded.
 */
internal fun DrawScope.drawBar(
    left: Float,
    baseline: Float,
    width: Float,
    height: Float,
    color: Color,
    upward: Boolean = true
) {
    if (height <= 0f) return

    val rounded = CornerRadius(min(BarCornerRadius.toPx(), min(width / 2f, height)))
    val square = CornerRadius.Zero
    val bar = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = if (upward) baseline - height else baseline,
                right = left + width,
                bottom = if (upward) baseline else baseline + height,
                topLeftCornerRadius = if (upward) rounded else square,
                topRightCornerRadius = if (upward) rounded else square,
                bottomRightCornerRadius = if (upward) square else rounded,
                bottomLeftCornerRadius = if (upward) square else rounded
            )
        )
    }
    drawPath(bar, color)
}

/** Text on a rounded patch of [backdrop]. */
internal fun DrawScope.drawLabel(
    label: TextLayoutResult,
    topLeft: Offset,
    backdrop: Color,
    alpha: Float = 1f
) {
    val padding = LabelSpacing.toPx()
    drawRoundRect(
        color = backdrop,
        topLeft = Offset(topLeft.x - padding, topLeft.y),
        size = Size(label.size.width + 2 * padding, label.size.height.toFloat()),
        cornerRadius = CornerRadius(padding),
        alpha = alpha
    )
    drawText(label, topLeft = topLeft, alpha = alpha)
}

/** Places the content at the top, centred on [fraction] of the width but kept inside the edges. */
internal fun Modifier.centeredOn(fraction: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val maxLeft = max(0, constraints.maxWidth - placeable.width)
    val left = (constraints.maxWidth * fraction - placeable.width / 2f).roundToInt().coerceIn(0, maxLeft)

    layout(constraints.maxWidth, placeable.height) {
        placeable.place(left, 0)
    }
}

/** The pill that floats over a chart's selected point. With [onClick] it shows a chevron. */
@Composable
internal fun ChartTooltip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null
) {
    val shape = RoundedCornerShape(12.dp)

    Surface(
        modifier = modifier
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface
    ) {
        Row(
            modifier = Modifier.padding(
                start = 12.dp,
                end = if (onClick != null) 6.dp else 12.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** One entry of a chart legend: a dot of [color] and what it stands for. */
@Composable
internal fun ChartLegendItem(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Legend entries side by side under a chart. */
@Composable
internal fun ChartLegend(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
