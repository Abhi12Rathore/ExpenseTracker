package com.example.expensetrackerpro.utils

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricAuthenticatorsTest {

    @Test
    fun api30AndAbove_strongBiometricOrScreenLock() {
        listOf(30, 33, 35).forEach { sdk ->
            assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, BiometricAuthenticators.forSdk(sdk))
            assertEquals(BIOMETRIC_STRONG, BiometricAuthenticators.biometricForSdk(sdk))
        }
    }

    @Test
    fun api28And29_weakBiometricOrScreenLock() {
        listOf(28, 29).forEach { sdk ->
            assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, BiometricAuthenticators.forSdk(sdk))
            assertEquals(BIOMETRIC_WEAK, BiometricAuthenticators.biometricForSdk(sdk))
        }
    }

    @Test
    fun api24To27_weakBiometricWithTheLibraryScreenLockFallback() {
        listOf(24, 27).forEach { sdk ->
            assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, BiometricAuthenticators.forSdk(sdk))
            assertEquals(BIOMETRIC_WEAK, BiometricAuthenticators.biometricForSdk(sdk))
        }
    }
}
