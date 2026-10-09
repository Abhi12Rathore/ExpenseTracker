package com.example.expensetrackerpro.utils

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

/** Which authenticators the app lock asks for on each Android version. */
object BiometricAuthenticators {

    private const val API_30 = 30

    /** Biometrics only, used to tell whether a biometric is ready to use. */
    fun biometricForSdk(sdkInt: Int): Int = if (sdkInt >= API_30) BIOMETRIC_STRONG else BIOMETRIC_WEAK

    /**
     * What the prompt accepts: a biometric or the screen lock. Strong biometrics can't be combined
     * with the screen lock before API 30, so older versions take weak ones; on API 24-27 the
     * library supplies the screen lock fallback itself.
     */
    fun forSdk(sdkInt: Int): Int = biometricForSdk(sdkInt) or DEVICE_CREDENTIAL
}
