package com.example.expensetrackerpro.presentation.common.motion

import androidx.compose.ui.graphics.ImageBitmap
import com.example.expensetrackerpro.domain.time.TimeProvider
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

/**
 * Holds a picture of the screen across the activity being created again, as a change of
 * language does, so the new activity can fade it out over itself instead of appearing at once.
 * Outlives the activity, not the process, and holds at most one picture for a second.
 */
@ActivityRetainedScoped
class ScreenSnapshotHolder @Inject constructor(
    private val timeProvider: TimeProvider
) {
    private var snapshot: ImageBitmap? = null
    private var takenAtMillis = 0L

    fun put(bitmap: ImageBitmap) {
        snapshot = bitmap
        takenAtMillis = nowMillis()
    }

    /**
     * The picture, handed out once. Null after [MAX_AGE_MILLIS]: by then whatever was meant to
     * replace it has long been on screen, so it is let go instead.
     */
    fun take(): ImageBitmap? {
        val taken = snapshot
        snapshot = null
        return taken?.takeIf { nowMillis() - takenAtMillis <= MAX_AGE_MILLIS }
    }

    private fun nowMillis(): Long = timeProvider.now().timeInMillis

    companion object {
        const val MAX_AGE_MILLIS = 1_000L
    }
}
