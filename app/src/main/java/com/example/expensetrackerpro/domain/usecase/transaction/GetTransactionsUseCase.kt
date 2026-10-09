package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    /**
     * Transactions dated within [range], newest first. A non-blank [query] keeps only those
     * whose title, note or category name contains it, ignoring case.
     */
    operator fun invoke(range: DateRange = DateRange.ALL, query: String = ""): Flow<List<Transaction>> =
        transactionRepository.getTransactions(range, query)
}
