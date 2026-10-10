package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import javax.inject.Inject

/** Puts a deleted transaction back under the id it had. */
class RestoreTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(transaction: Transaction): Outcome<Unit> =
        transactionRepository.saveTransaction(transaction)
}
