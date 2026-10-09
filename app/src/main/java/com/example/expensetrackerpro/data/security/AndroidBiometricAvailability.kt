package com.example.expensetrackerpro.data.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.security.BiometricAvailability
import com.example.expensetrackerpro.utils.BiometricAuthenticators
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidBiometricAvailability @Inject constructor(
    @ApplicationContext private val context: Context
) : BiometricAvailability {

    override fun status(): BiometricStatus {
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isDeviceSecure != true) return BiometricStatus.NO_DEVICE_CREDENTIAL

        val authenticators = BiometricAuthenticators.biometricForSdk(Build.VERSION.SDK_INT)
        return when (BiometricManager.from(context).canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.NO_HARDWARE
        }
    }
}
