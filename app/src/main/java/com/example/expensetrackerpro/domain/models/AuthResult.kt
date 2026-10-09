package com.example.expensetrackerpro.domain.models

/** How an authentication prompt ended. */
enum class AuthResult {
    SUCCESS,
    CANCELED,
    // Too many failed attempts; biometrics come back after a short wait
    LOCKOUT,
    // Biometrics stay off until the device is unlocked with its screen lock
    LOCKOUT_PERMANENT,
    // The device no longer has a screen lock or biometric to authenticate with
    NO_CREDENTIAL,
    ERROR
}
