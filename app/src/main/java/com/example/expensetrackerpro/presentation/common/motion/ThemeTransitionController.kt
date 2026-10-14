package com.example.expensetrackerpro.presentation.common.motion

import androidx.compose.ui.geometry.Offset
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.motion.MotionPreferences
import com.example.expensetrackerpro.domain.time.TimeProvider
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

/**
 * Carries the spot the user tapped on a theme option to the app's root, which reveals the new
 * theme as a circle growing from there. A choice that leaves the app looking the same (System
 * while the phone is already dark, say) asks for nothing.
 */
@ActivityRetainedScoped
class ThemeTransitionController @Inject constructor(
    private val timeProvider: TimeProvider,
    private val motion: MotionPreferences
) {

    private data class PendingReveal(val target: ThemeMode, val center: Offset, val tappedAtMillis: Long)

    private var pending: PendingReveal? = null

    /** Whether [mode] draws the app dark, given whether the phone itself is dark. */
    fun effectiveDark(mode: ThemeMode, phoneDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> phoneDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    /**
     * The user picked [to] while [from] was applied, pressing at [center] in root coordinates.
     * [phoneDark] is null when the phone's dark mode can't be told (it follows a schedule): a
     * change to or from System is then taken to change the look, and if it doesn't, the tap
     * expires unused.
     */
    fun onOptionTapped(from: ThemeMode, to: ThemeMode, phoneDark: Boolean?, center: Offset) {
        if (!motion.areAnimationsEnabled()) {
            // Nothing is revealed: the root switches the theme at once
            pending = null
            return
        }
        val changesLook = if (phoneDark == null) {
            from != to
        } else {
            effectiveDark(from, phoneDark) != effectiveDark(to, phoneDark)
        }
        pending = if (changesLook) PendingReveal(to, center, nowMillis()) else null
    }

    /**
     * Where to grow the reveal from now that [mode] is applied, handed out once. Null when the
     * change didn't come from a recent tap on that option, as when the phone switched dark mode:
     * such a change cross-fades instead. A tap whose change never came (the save failed, or the
     * look turned out the same) is forgotten after [TAP_VALID_MILLIS].
     */
    fun takeReveal(mode: ThemeMode): Offset? {
        val reveal = pending
        pending = null
        return reveal
            ?.takeIf { it.target == mode && nowMillis() - it.tappedAtMillis <= TAP_VALID_MILLIS }
            ?.center
    }

    private fun nowMillis(): Long = timeProvider.now().timeInMillis

    companion object {
        // Long enough for the choice to be saved and the phone's night mode to follow
        const val TAP_VALID_MILLIS = 1_000L
    }
}
