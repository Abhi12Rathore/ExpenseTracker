package com.example.expensetrackerpro.presentation.common.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.example.expensetrackerpro.domain.models.ThemeMode
import kotlin.math.hypot

private const val REVEAL_MILLIS = 450
private const val CROSS_FADE_MILLIS = 250
private const val RECREATE_FADE_MILLIS = 300

/** How a theme option reaches the root's reveal. Null outside the app, as in previews. */
val LocalThemeTransition = staticCompositionLocalOf<ThemeTransitionController?> { null }

/** Takes the picture of the screen that the next activity fades out. Null outside the app. */
val LocalScreenSnapshot = staticCompositionLocalOf<ScreenSnapshot?> { null }

class ScreenSnapshot internal constructor(
    private val layer: GraphicsLayer,
    private val holder: ScreenSnapshotHolder
) {
    /** Call just before something recreates the activity, such as applying a language. */
    suspend fun captureForRecreate() {
        holder.put(layer.toImageBitmap())
    }
}

// The old look, drawn over the new one until it has gone
private class Overlay(val snapshot: ImageBitmap, val revealCenter: Offset?)

/**
 * The app's root. A theme change is shown by drawing a snapshot of the old look over the new one
 * and taking it away: as a growing circle from the tapped option, or as a cross-fade when the
 * phone switched dark mode. The whole tree recomposes with the new colors in one frame, so the
 * snapshot is what makes the change visible as motion. The overlay is only drawn, so taps reach
 * the screen beneath it at once.
 *
 * When the activity has just been created again (a change of language), the picture the previous
 * one left in [snapshots] is faded out over the new screen, so the new language appears without
 * a flash or a jump.
 *
 * [content] is given the theme to draw and, separately, the one the status-bar icons follow, which
 * switch only once the reveal has finished.
 */
@Composable
fun AppMotionHost(
    themeMode: ThemeMode,
    darkTheme: Boolean,
    // False until the saved theme is known, so the first one is applied without motion
    animate: Boolean,
    controller: ThemeTransitionController,
    snapshots: ScreenSnapshotHolder,
    content: @Composable (shownDark: Boolean, systemBarsDark: Boolean) -> Unit
) {
    val layer = rememberGraphicsLayer()
    var shownDark by remember { mutableStateOf(darkTheme) }
    var systemBarsDark by remember { mutableStateOf(darkTheme) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val revealRadius = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }
    val screenSnapshot = remember(layer, snapshots) { ScreenSnapshot(layer, snapshots) }

    // Left by the activity before this one, if it was just recreated
    var previousScreen by remember { mutableStateOf(snapshots.take()) }
    val previousScreenAlpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (previousScreen == null) return@LaunchedEffect
        try {
            previousScreenAlpha.animateTo(0f, tween(RECREATE_FADE_MILLIS))
        } finally {
            previousScreen = null
        }
    }

    // Keyed on the theme, so a second change cancels the first: its `finally` jumps it to the
    // end, and the new one starts from what is on screen. Reveals never stack.
    LaunchedEffect(darkTheme) {
        if (darkTheme == shownDark) return@LaunchedEffect
        val center = controller.takeReveal(themeMode)
        try {
            if (!animate) return@LaunchedEffect
            // Taken before the switch, so it always shows the old look
            val snapshot = layer.toImageBitmap()
            revealRadius.snapTo(0f)
            overlayAlpha.snapTo(1f)
            overlay = Overlay(snapshot, center)
            shownDark = darkTheme
            if (center != null) {
                val diagonal = hypot(layer.size.width.toFloat(), layer.size.height.toFloat())
                revealRadius.animateTo(diagonal, tween(REVEAL_MILLIS, easing = EaseOut))
            } else {
                overlayAlpha.animateTo(0f, tween(CROSS_FADE_MILLIS))
            }
        } finally {
            shownDark = darkTheme
            systemBarsDark = darkTheme
            // The bitmap is let go as soon as it isn't drawn
            overlay = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
                previousScreen?.let { drawImage(it, alpha = previousScreenAlpha.value) }
                val shown = overlay ?: return@drawWithContent
                val center = shown.revealCenter
                if (center != null) {
                    val hole = Path().apply { addOval(Rect(center, revealRadius.value)) }
                    clipPath(hole, ClipOp.Difference) { drawImage(shown.snapshot) }
                } else {
                    drawImage(shown.snapshot, alpha = overlayAlpha.value)
                }
            }
    ) {
        CompositionLocalProvider(
            LocalThemeTransition provides controller,
            LocalScreenSnapshot provides screenSnapshot
        ) {
            content(shownDark, systemBarsDark)
        }
    }
}
