package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import javax.inject.Inject

class UndoCurrencyChangeUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {

    /** Back to the currency shown before the last change. Does nothing once that is forgotten. */
    suspend operator fun invoke() {
        val change = currencyRepository.pendingChange.value ?: return

        // Cleared first, so going back is not itself offered an Undo
        currencyRepository.setPendingChange(null)
        currencyRepository.setCurrency(change.previous)
    }
}
