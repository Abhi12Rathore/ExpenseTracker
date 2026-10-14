package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.motion.MotionPreferences

/** Animations on unless a test turns them off, as "Remove animations" does. */
class FakeMotionPreferences(var enabled: Boolean = true) : MotionPreferences {
    override fun areAnimationsEnabled(): Boolean = enabled
}
