package com.example.expensetrackerpro.presentation.common.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.example.expensetrackerpro.domain.models.Money
import kotlinx.coroutines.flow.first
import kotlin.math.roundToLong

private const val COUNT_UP_MILLIS = 600

/**
 * [target], counted up from zero the first time [start] is true, and [target] as it is after
 * that: a later change (another period picked) shows at once. Saved with the screen, so coming
 * back to it doesn't count again. The value is whole paise at every frame, to be formatted as
 * any other amount.
 */
@Composable
fun rememberCountUp(target: Money, start: Boolean): Money {
    // With animations off it has, in effect, already played
    val animationsEnabled = LocalAnimationsEnabled.current
    var played by rememberSaveable { mutableStateOf(!animationsEnabled) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    val currentStart by rememberUpdatedState(start)

    // Keyed on nothing, so the count isn't cut short if loading starts again halfway through
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        snapshotFlow { currentStart }.first { it }
        played = true
        progress.animateTo(1f, tween(COUNT_UP_MILLIS, easing = FastOutSlowInEasing))
    }

    // In a Double, which holds every paisa of any amount; a Float would round the last digits off
    return Money((target.paise * progress.value.toDouble()).roundToLong())
}
