package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.models.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferenceRepository {

    fun getThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
