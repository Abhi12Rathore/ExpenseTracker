package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCurrencyChangeUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {

    /** The currency change that can still be undone; null once it was undone or forgotten. */
    operator fun invoke(): Flow<CurrencyChange?> = currencyRepository.pendingChange
}
