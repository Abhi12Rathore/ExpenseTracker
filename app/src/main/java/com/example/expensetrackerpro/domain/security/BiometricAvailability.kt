package com.example.expensetrackerpro.domain.security

import com.example.expensetrackerpro.domain.models.BiometricStatus

interface BiometricAvailability {

    /** Read fresh on every call, since the user can change it in system settings at any time. */
    fun status(): BiometricStatus
}
