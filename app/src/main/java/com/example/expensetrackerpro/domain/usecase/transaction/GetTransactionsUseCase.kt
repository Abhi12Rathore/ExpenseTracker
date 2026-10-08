package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    /** Transactions dated at or after [startTimestamp], newest first. */
    operator fun invoke(startTimestamp: Long = 0L): Flow<List<Transaction>> =
        transactionRepository.getFilteredTransactions(startTimestamp)
}
