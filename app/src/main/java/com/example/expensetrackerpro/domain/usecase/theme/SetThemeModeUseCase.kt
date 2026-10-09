package com.example.expensetrackerpro.domain.usecase.theme

import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.repository.ThemePreferenceRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val themePreferenceRepository: ThemePreferenceRepository
) {

    suspend operator fun invoke(mode: ThemeMode) {
        themePreferenceRepository.setThemeMode(mode)
    }
}
