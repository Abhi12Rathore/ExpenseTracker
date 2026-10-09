package com.example.expensetrackerpro.domain.usecase.applock

import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.repository.AppLockRepository
import javax.inject.Inject

class SetAppLockSettingsUseCase @Inject constructor(
    private val appLockRepository: AppLockRepository
) {

    suspend operator fun invoke(settings: AppLockSettings) {
        appLockRepository.setSettings(settings)
    }
}
