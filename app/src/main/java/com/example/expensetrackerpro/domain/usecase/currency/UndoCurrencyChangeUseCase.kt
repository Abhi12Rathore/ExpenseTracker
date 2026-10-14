package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.onFailure
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import javax.inject.Inject

class UndoCurrencyChangeUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {

    /**
     * Back to the currency shown before the last change. Does nothing once that is forgotten.
     * When going back can't be stored, the change stays, as does its Undo.
     */
    suspend operator fun invoke(): Outcome<Unit> {
        val change = currencyRepository.pendingChange.value ?: return Outcome.Success(Unit)

        // Cleared first, so going back is not itself offered an Undo
        currencyRepository.setPendingChange(null)
        return currencyRepository.setCurrency(change.previous).onFailure {
            currencyRepository.setPendingChange(change)
        }
    }
}
