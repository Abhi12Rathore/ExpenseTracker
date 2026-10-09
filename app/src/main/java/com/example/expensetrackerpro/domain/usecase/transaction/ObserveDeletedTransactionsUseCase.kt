package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDeletedTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    /** Each transaction as it is deleted, wherever in the app that happens. */
    operator fun invoke(): Flow<Transaction> = transactionRepository.deletions
}
