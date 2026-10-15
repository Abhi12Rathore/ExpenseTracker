package com.example.expensetrackerpro.data.speech

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import com.example.expensetrackerpro.domain.speech.VoiceInputAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Whether an app can show the system's "Speak now" screen. That screen records the voice itself,
 * so this app needs no microphone permission. Seen through the manifest's `<queries>` entry.
 */
class AndroidVoiceInputAvailability @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceInputAvailability {

    override fun isAvailable(): Boolean =
        context.packageManager
            .queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
}
