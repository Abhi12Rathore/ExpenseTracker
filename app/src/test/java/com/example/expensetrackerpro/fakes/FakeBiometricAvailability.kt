package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.security.BiometricAvailability

class FakeBiometricAvailability(
    var current: BiometricStatus = BiometricStatus.AVAILABLE
) : BiometricAvailability {

    override fun status(): BiometricStatus = current
}
