package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionListSummary
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import com.example.expensetrackerpro.domain.models.sumOfMoney
import javax.inject.Inject

class SummarizeTransactionsUseCase @Inject constructor() {

    /**
     * The figures for the [transactions] listed. Search results get their own summary whatever
     * the type filter; otherwise it follows [typeFilter], which [transactions] already match.
     */
    operator fun invoke(
        transactions: List<Transaction>,
        typeFilter: TransactionTypeFilter,
        isSearching: Boolean
    ): TransactionListSummary = when {
        isSearching -> {
            val total = transactions.sumOfMoney { it.signedAmount }

            TransactionListSummary.Search(
                count = transactions.size,
                total = total,
                average = if (transactions.isEmpty()) Money.ZERO else total / transactions.size
            )
        }
        typeFilter == TransactionTypeFilter.EXPENSE -> singleType(TransactionType.EXPENSE, transactions)
        typeFilter == TransactionTypeFilter.INCOME -> singleType(TransactionType.INCOME, transactions)
        else -> {
            val income = transactions.totalOf(TransactionType.INCOME)
            val expense = transactions.totalOf(TransactionType.EXPENSE)

            TransactionListSummary.Overview(income = income, expense = expense, net = income - expense)
        }
    }

    private fun singleType(type: TransactionType, transactions: List<Transaction>) =
        TransactionListSummary.SingleType(
            type = type,
            total = transactions.sumOfMoney { it.amount },
            count = transactions.size,
            biggest = transactions.maxOfOrNull { it.amount } ?: Money.ZERO
        )

    private fun List<Transaction>.totalOf(type: TransactionType): Money =
        filter { it.transactionType == type }.sumOfMoney { it.amount }
}
