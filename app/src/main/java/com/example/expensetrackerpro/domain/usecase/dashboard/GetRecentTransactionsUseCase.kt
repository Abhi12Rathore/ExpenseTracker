package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRecentTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    /** The [limit] newest transactions within [range]. */
    operator fun invoke(range: DateRange = DateRange.ALL, limit: Int = 5): Flow<List<Transaction>> =
        transactionRepository.getTransactions(range).map { it.take(limit) }
}
