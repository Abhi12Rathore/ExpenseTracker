package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import javax.inject.Inject

class FilterTransactionsUseCase @Inject constructor() {

    /** Keeps the transactions of [typeFilter]'s type and, when [categoryId] is given, of that category. */
    operator fun invoke(
        transactions: List<Transaction>,
        typeFilter: TransactionTypeFilter,
        categoryId: Long? = null
    ): List<Transaction> {
        val type = when (typeFilter) {
            TransactionTypeFilter.ALL -> null
            TransactionTypeFilter.EXPENSE -> TransactionType.EXPENSE
            TransactionTypeFilter.INCOME -> TransactionType.INCOME
        }

        return transactions.filter { transaction ->
            (type == null || transaction.transactionType == type) &&
                (categoryId == null || transaction.categoryId.toLong() == categoryId)
        }
    }
}
