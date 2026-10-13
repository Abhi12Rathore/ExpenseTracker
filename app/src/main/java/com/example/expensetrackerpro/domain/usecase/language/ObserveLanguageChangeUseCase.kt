package com.example.expensetrackerpro.domain.usecase.language

import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.repository.AppLanguageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLanguageChangeUseCase @Inject constructor(
    private val appLanguageRepository: AppLanguageRepository
) {

    /** The language change that can still be undone; null once it was undone or forgotten. */
    operator fun invoke(): Flow<LanguageChange?> = appLanguageRepository.pendingChange
}
