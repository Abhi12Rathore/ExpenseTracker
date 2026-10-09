package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.models.AppLockSettings
import kotlinx.coroutines.flow.Flow

interface AppLockRepository {

    fun getSettings(): Flow<AppLockSettings>

    suspend fun setSettings(settings: AppLockSettings)
}
