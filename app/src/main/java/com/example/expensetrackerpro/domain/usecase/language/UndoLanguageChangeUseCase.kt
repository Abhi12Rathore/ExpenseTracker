package com.example.expensetrackerpro.domain.usecase.language

import com.example.expensetrackerpro.domain.repository.AppLanguageRepository
import javax.inject.Inject

class UndoLanguageChangeUseCase @Inject constructor(
    private val appLanguageRepository: AppLanguageRepository
) {

    /** Back to the language the app was shown in before the last change. Does nothing once that is forgotten. */
    operator fun invoke() {
        val change = appLanguageRepository.pendingChange.value ?: return

        // Cleared first, so going back is not itself offered an Undo
        appLanguageRepository.setPendingChange(null)
        appLanguageRepository.setChoice(change.previous)
    }
}
