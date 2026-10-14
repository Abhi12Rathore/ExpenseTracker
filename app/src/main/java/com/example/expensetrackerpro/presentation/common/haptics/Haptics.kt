package com.example.expensetrackerpro.presentation.common.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * A short tap of the phone to confirm an action. Through the View rather than Compose's
 * `HapticFeedbackType`, which in this Compose version has no confirm or toggle feedback. Each
 * falls back to an older constant where the phone's Android lacks it. The phone's own touch
 * feedback setting is respected by the system.
 */
class Haptics internal constructor(private val view: View) {

    /** Something was done for good: a choice applied, a transaction saved. */
    fun confirm() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
        )
    }

    /** A switch was turned [on] or off. */
    fun toggle(on: Boolean) {
        view.performHapticFeedback(
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                else -> HapticFeedbackConstants.CLOCK_TICK
            }
        )
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
