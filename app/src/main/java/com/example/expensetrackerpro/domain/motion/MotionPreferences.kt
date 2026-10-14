package com.example.expensetrackerpro.domain.motion

/** Whether the user wants things on screen to move, read from the phone's settings. */
interface MotionPreferences {

    /**
     * False when animations are turned off ("Remove animations", or an animator duration scale
     * of 0): every transition is then skipped and changes appear at once.
     */
    fun areAnimationsEnabled(): Boolean
}
