package com.example.expensetrackerpro.presentation.common.motion

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** Remembers where a control was last pressed, in root coordinates, without taking the press. */
class PressTracker {
    internal var coordinates: LayoutCoordinates? = null
    internal var lastPress: Offset? = null

    /**
     * The last press, handed out once; the control's centre when it was chosen without a press,
     * as with TalkBack or a keyboard.
     */
    fun take(): Offset {
        val press = lastPress
        lastPress = null
        val bounds = coordinates?.takeIf { it.isAttached }?.boundsInRoot()
        return press ?: bounds?.center ?: Offset.Zero
    }
}

@Composable
fun rememberPressTracker(): PressTracker = remember { PressTracker() }

fun Modifier.trackPresses(tracker: PressTracker): Modifier = this
    .onGloballyPositioned { tracker.coordinates = it }
    .pointerInput(tracker) {
        awaitEachGesture {
            // The initial pass sees the press before the control does; nothing is consumed
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.firstOrNull { it.changedToDown() } ?: return@awaitEachGesture
            tracker.lastPress = tracker.coordinates?.takeIf { it.isAttached }?.localToRoot(down.position)
        }
    }
