package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.voice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.example.expensetrackerpro.R

/**
 * The system's "Speak now" screen, listening in [languageTag]. That screen records the voice
 * itself, so the app needs no microphone permission.
 *
 * Returns what opens it. [onRecognized] gets what was heard; backing out calls nothing.
 * [onUnavailable] is called when no app can show the screen after all.
 */
@Composable
fun rememberVoiceInput(
    languageTag: String,
    onRecognized: (String) -> Unit,
    onUnavailable: () -> Unit
): () -> Unit {
    val prompt = stringResource(R.string.quick_add_voice_prompt)
    val currentOnRecognized by rememberUpdatedState(onRecognized)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull { it.isNotBlank() }
                ?.let { heard -> currentOnRecognized(heard) }
        }
    }

    return remember(launcher, languageTag, prompt) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                // The recognizer was there when the screen opened, and has gone since
                currentOnUnavailable()
            }
        }
    }
}
