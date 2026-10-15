package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import javax.inject.Inject

class LoadCurrencyUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {

    /** Reads the chosen currency, choosing and storing the phone region's on a fresh install. */
    suspend operator fun invoke() = currencyRepository.load()
}
