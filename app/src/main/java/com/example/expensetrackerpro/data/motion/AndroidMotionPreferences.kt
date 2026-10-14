package com.example.expensetrackerpro.data.motion

import android.content.Context
import android.provider.Settings
import com.example.expensetrackerpro.domain.motion.MotionPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads the animator duration scale, which both "Remove animations" (Accessibility) and the
 * developer option set. Read on every call, so a change made while the app was away counts.
 */
class AndroidMotionPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) : MotionPreferences {

    override fun areAnimationsEnabled(): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}
