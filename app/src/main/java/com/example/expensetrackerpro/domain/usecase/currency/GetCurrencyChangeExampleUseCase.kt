package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.models.CurrencyChangeExample
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import javax.inject.Inject

class GetCurrencyChangeExampleUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    /** The latest transaction and how many there are, or null when there are none: nothing to explain then. */
    suspend operator fun invoke(): CurrencyChangeExample? {
        val latest = transactionRepository.getLatestTransaction() ?: return null

        return CurrencyChangeExample(
            transactionCount = transactionRepository.countTransactions(),
            latestAmount = latest.amount
        )
    }
}
