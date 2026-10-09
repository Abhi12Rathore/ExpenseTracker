package com.example.expensetrackerpro.domain.usecase.theme

import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.repository.ThemePreferenceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(
    private val themePreferenceRepository: ThemePreferenceRepository
) {

    operator fun invoke(): Flow<ThemeMode> = themePreferenceRepository.getThemeMode()
}
