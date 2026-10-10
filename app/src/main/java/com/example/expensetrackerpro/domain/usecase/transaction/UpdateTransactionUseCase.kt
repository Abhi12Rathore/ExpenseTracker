package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(transaction: Transaction): Outcome<Unit> =
        transactionRepository.updateTransaction(transaction)
}
