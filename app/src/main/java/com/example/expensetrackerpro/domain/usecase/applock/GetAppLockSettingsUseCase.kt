package com.example.expensetrackerpro.domain.usecase.applock

import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppLockSettingsUseCase @Inject constructor(
    private val appLockRepository: AppLockRepository
) {

    operator fun invoke(): Flow<AppLockSettings> = appLockRepository.getSettings()
}
