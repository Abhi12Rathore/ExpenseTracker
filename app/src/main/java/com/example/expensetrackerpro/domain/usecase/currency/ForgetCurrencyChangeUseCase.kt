package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import javax.inject.Inject

class ForgetCurrencyChangeUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {

    /** The last currency change stays: it is no longer offered an Undo. */
    operator fun invoke() = currencyRepository.setPendingChange(null)
}
