package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.repository.ThemePreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeThemePreferenceRepository(
    initial: ThemeMode = ThemeMode.SYSTEM
) : ThemePreferenceRepository {

    private val themeMode = MutableStateFlow(initial)

    val current: ThemeMode get() = themeMode.value

    override fun getThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}
