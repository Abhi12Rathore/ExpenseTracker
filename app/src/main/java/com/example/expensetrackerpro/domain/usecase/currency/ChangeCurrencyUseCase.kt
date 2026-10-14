package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import javax.inject.Inject

class ChangeCurrencyUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {

    /** Shows amounts in [currency] and remembers the one before, so the change can be undone. */
    suspend operator fun invoke(currency: AppCurrency) {
        val previous = currencyRepository.currency.value
        if (previous == currency) return

        currencyRepository.setCurrency(currency)
        currencyRepository.setPendingChange(CurrencyChange(previous = previous, current = currency))
    }
}
