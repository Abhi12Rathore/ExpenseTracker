package com.example.expensetrackerpro.domain.models

enum class BiometricStatus {
    AVAILABLE,
    // The two below still unlock with the screen lock
    NO_HARDWARE,
    NONE_ENROLLED,
    // No PIN, pattern or password is set, so nothing can authenticate
    NO_DEVICE_CREDENTIAL
}
