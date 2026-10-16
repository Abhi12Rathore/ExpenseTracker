package com.example.expensetrackerpro.domain.speech

fun interface VoiceInputAvailability {

    /** Whether the phone has a speech recognizer that the system's "Speak now" screen can use. */
    fun isAvailable(): Boolean
}
