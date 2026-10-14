package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import javax.inject.Inject

class SetCurrencyUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {

    /** Shows amounts in [currency] from now on. Nothing stored is converted. */
    suspend operator fun invoke(currency: AppCurrency) = currencyRepository.setCurrency(currency)
}
