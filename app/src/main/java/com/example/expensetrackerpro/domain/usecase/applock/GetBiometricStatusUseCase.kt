package com.example.expensetrackerpro.domain.usecase.applock

import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.security.BiometricAvailability
import javax.inject.Inject

class GetBiometricStatusUseCase @Inject constructor(
    private val biometricAvailability: BiometricAvailability
) {

    operator fun invoke(): BiometricStatus = biometricAvailability.status()
}
