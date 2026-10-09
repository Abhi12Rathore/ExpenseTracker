package com.example.expensetrackerpro.presentation.lock

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.utils.BiometricAuthenticators

/** Shows the system biometric / screen lock prompt and reports how it ended. */
class BiometricAuthenticator(
    activity: FragmentActivity,
    onResult: (AuthResult) -> Unit
) {

    // Created up front: after a rotation this hands a prompt that is still open its new callback
    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(AuthResult.SUCCESS)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(errorCode.toAuthResult())
            }

            // onAuthenticationFailed is one rejected attempt; the prompt stays open, so nothing ends
        }
    )

    fun authenticate(title: String, subtitle: String) {
        prompt.authenticate(
            // No negative button: the screen lock fallback takes its place
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BiometricAuthenticators.forSdk(Build.VERSION.SDK_INT))
                .build()
        )
    }

    private fun Int.toAuthResult(): AuthResult = when (this) {
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        BiometricPrompt.ERROR_CANCELED -> AuthResult.CANCELED
        BiometricPrompt.ERROR_LOCKOUT -> AuthResult.LOCKOUT
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> AuthResult.LOCKOUT_PERMANENT
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
        BiometricPrompt.ERROR_NO_BIOMETRICS -> AuthResult.NO_CREDENTIAL
        else -> AuthResult.ERROR
    }
}

/** Null when the host is not a [FragmentActivity], as in previews. */
@Composable
fun rememberBiometricAuthenticator(onResult: (AuthResult) -> Unit): BiometricAuthenticator? {
    val activity = LocalActivity.current as? FragmentActivity
    val currentOnResult by rememberUpdatedState(onResult)
    return remember(activity) {
        activity?.let { BiometricAuthenticator(it) { result -> currentOnResult(result) } }
    }
}
