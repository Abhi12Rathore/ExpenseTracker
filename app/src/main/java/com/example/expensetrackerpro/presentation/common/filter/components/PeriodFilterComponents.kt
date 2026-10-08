package com.example.expensetrackerpro.presentation.common.filter.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R

/*
 * Home/History period filter, matching design option C:
 *
 *   [ Week | (Month) | Year ]  [📅]
 *   ┌──────────────────────────────┐
 *   │ [‹]    October 2026     [›]  │
 *   │          1 – 31 Oct          │
 *   └──────────────────────────────┘
 *
 * Colors come from MaterialTheme roles, so the theme MUST define them
 * (dynamic color off). See the theme notes at the bottom of this file.
 */

private val TrackShape = RoundedCornerShape(14.dp)
private val PillShape = RoundedCornerShape(11.dp)
private val IconButtonShape = RoundedCornerShape(12.dp)
private val StepperShape = RoundedCornerShape(16.dp)

/** "Raised" surface: white in light mode, a lighter grey in dark mode (so it still reads as raised). */
@Composable
private fun raisedSurfaceColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (colors.surface.luminance() < 0.5f) colors.surfaceContainerHighest
    else colors.surfaceContainerLowest
}

/** Segmented control + calendar button on one row. */
@Composable
fun <T> PeriodFilterBar(
    options: List<T>,
    selected: T?,                 // null = custom range active, no segment highlighted
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onCustomRangeClick: () -> Unit,
    isCustomActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillSegmentedControl(
            options = options,
            selected = selected,
            label = label,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        TonalSquareIconButton(
            icon = Icons.Rounded.CalendarMonth,
            contentDescription = stringResource(R.string.filter_custom_range),
            onClick = onCustomRangeClick,
            highlighted = isCustomActive,
        )
    }
}

/** Grey track with a white pill that slides under the selected option. */
@Composable
fun <T> PillSegmentedControl(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val selectedIndex = options.indexOf(selected)

    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .clip(TrackShape)
            .background(colors.surfaceContainer)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex.coerceAtLeast(0),
            animationSpec = tween(250),
            label = "indicatorOffset",
        )
        val indicatorAlpha by animateFloatAsState(
            targetValue = if (selectedIndex >= 0) 1f else 0f,
            label = "indicatorAlpha",
        )

        // Sliding white pill
        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(segmentWidth)
                .fillMaxHeight()
                .alpha(indicatorAlpha)
                .shadow(elevation = 1.dp, shape = PillShape)
                .background(raisedSurfaceColor(), PillShape),
        )

        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(PillShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // the sliding pill is the feedback
                        ) { onSelect(option) }
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) colors.primary else colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 44dp rounded-square icon button with a soft tinted background. */
@Composable
fun TonalSquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(IconButtonShape)
            .background(if (highlighted) colors.primary else colors.secondaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (highlighted) colors.onPrimary else colors.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * White bordered card: ‹  title / subtitle  ›
 * Swipe left/right on it to step too.
 */
@Composable
fun PeriodStepper(
    title: String,
    subtitle: String?,            // null when the title already is the day range
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    showArrows: Boolean = true,   // false in custom-range mode
) {
    val colors = MaterialTheme.colorScheme
    val dragTotal = remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(canGoPrevious, canGoNext, showArrows) {
                if (!showArrows) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal.floatValue = 0f },
                    onDragEnd = {
                        val threshold = 60.dp.toPx()
                        when {
                            dragTotal.floatValue > threshold && canGoPrevious -> onPrevious()
                            dragTotal.floatValue < -threshold && canGoNext -> onNext()
                        }
                    },
                ) { _, dragAmount -> dragTotal.floatValue += dragAmount }
            },
        shape = StepperShape,
        color = raisedSurfaceColor(),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperArrow(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.filter_previous_period),
                enabled = canGoPrevious,
                visible = showArrows,
                onClick = onPrevious,
            )

            AnimatedContent(
                targetState = title to subtitle,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally { -it / 4 } + fadeOut(tween(150)))
                },
                modifier = Modifier.weight(1f),
                label = "periodTitle",
            ) { (t, s) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = t,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (s != null) {
                        Text(
                            text = s,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            StepperArrow(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.filter_next_period),
                enabled = canGoNext,
                visible = showArrows,
                onClick = onNext,
            )
        }
    }
}

@Composable
private fun StepperArrow(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    visible: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .alpha(if (visible) 1f else 0f)
            .clip(IconButtonShape)
            .background(colors.surfaceContainerLow)
            .clickable(enabled = enabled && visible, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) colors.primary else colors.onSurface.copy(alpha = 0.25f),
        )
    }
}

/*
 * ─── Theme notes (ui/theme/Theme.kt) ────────────────────────────────────────
 * 1. Turn OFF dynamic color, otherwise Android 12+ replaces the palette with
 *    wallpaper colors (that is where the lavender cards come from):
 *        ExpenseTrackerProTheme(dynamicColor = false, ...)
 *
 * 2. Set these roles explicitly in lightColorScheme / darkColorScheme:
 *
 *    role                    light      dark
 *    background / surface    #FAFAF8    #111411
 *    surfaceContainerLowest  #FFFFFF    #0C0F0C
 *    surfaceContainerLow     #F4F6F3    #191C19
 *    surfaceContainer        #EEF1ED    #1D211D   <- chart cards + segment track
 *    surfaceContainerHigh    #E8ECE7    #272B27
 *    surfaceContainerHighest #E2E6E1    #323632
 *    outlineVariant          #DFE4DE    #3F493F
 *    secondaryContainer      #E3EEE2    #23402A
 *    onSecondaryContainer    #1D4D1C    #A8E6B4
 *
 * 3. Chart cards: use colorScheme.surfaceContainer as their background
 *    (not surfaceVariant / hardcoded colors).
 */
