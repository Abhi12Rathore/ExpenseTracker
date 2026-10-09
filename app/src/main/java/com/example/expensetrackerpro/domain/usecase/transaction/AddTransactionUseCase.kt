package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    /** Returns false when the transaction could not be written. */
    suspend operator fun invoke(transaction: Transaction): Boolean =
        transactionRepository.saveTransaction(transaction)
}
