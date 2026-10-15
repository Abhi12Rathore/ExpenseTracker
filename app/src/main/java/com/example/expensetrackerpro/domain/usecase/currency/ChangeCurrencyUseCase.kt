package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.onSuccess
import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import javax.inject.Inject

class ChangeCurrencyUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {

    /**
     * Shows amounts in [currency] and remembers the one before, so the change can be undone.
     * A change that couldn't be stored isn't made, and isn't offered an Undo.
     */
    suspend operator fun invoke(currency: AppCurrency): Outcome<Unit> {
        val previous = currencyRepository.currency.value
        if (previous == currency) return Outcome.Success(Unit)

        return currencyRepository.setCurrency(currency).onSuccess {
            currencyRepository.setPendingChange(CurrencyChange(previous = previous, current = currency))
        }
    }
}
