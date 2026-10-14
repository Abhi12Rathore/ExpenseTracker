package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveCurrencyUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {

    operator fun invoke(): StateFlow<AppCurrency> = currencyRepository.currency
}
