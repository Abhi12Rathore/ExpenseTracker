package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    /** Transactions dated within [range], newest first. */
    operator fun invoke(range: DateRange = DateRange.ALL): Flow<List<Transaction>> =
        transactionRepository.getTransactions(range)
}
