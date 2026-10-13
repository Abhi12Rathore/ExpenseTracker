package com.example.expensetrackerpro.domain.usecase.language

import com.example.expensetrackerpro.domain.repository.AppLanguageRepository
import javax.inject.Inject

class ForgetLanguageChangeUseCase @Inject constructor(
    private val appLanguageRepository: AppLanguageRepository
) {

    /** The last language change stays: it is no longer offered an Undo. */
    operator fun invoke() = appLanguageRepository.setPendingChange(null)
}
