package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppLockRepository(
    initial: AppLockSettings = AppLockSettings()
) : AppLockRepository {

    private val settings = MutableStateFlow(initial)

    val current: AppLockSettings get() = settings.value

    override fun getSettings(): Flow<AppLockSettings> = settings

    override suspend fun setSettings(settings: AppLockSettings) {
        this.settings.value = settings
    }
}
