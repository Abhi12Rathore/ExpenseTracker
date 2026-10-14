package com.example.expensetrackerpro.presentation.common.motion

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether Composables may animate, from the injected `MotionPreferences`, read again each time
 * the app comes back. When false every transition is skipped and changes appear at once. True
 * outside the app, as in previews.
 */
val LocalAnimationsEnabled = compositionLocalOf { true }
