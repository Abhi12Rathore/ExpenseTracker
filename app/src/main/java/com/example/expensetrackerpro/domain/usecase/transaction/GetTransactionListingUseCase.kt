package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionListing
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.TransactionTypeCounts
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import javax.inject.Inject

/** Turns the transactions of a period (and search) into what the Transactions list shows. */
class GetTransactionListingUseCase @Inject constructor(
    private val filterTransactions: FilterTransactionsUseCase,
    private val groupTransactionsByDay: GroupTransactionsByDayUseCase,
    private val summarizeTransactions: SummarizeTransactionsUseCase
) {

    /**
     * [transactions] are newest first. The counts ignore [typeFilter], so each type chip shows
     * what choosing it would list; the summary and the days cover only what is listed.
     */
    operator fun invoke(
        transactions: List<Transaction>,
        typeFilter: TransactionTypeFilter,
        categoryId: CategoryId?,
        isSearching: Boolean
    ): TransactionListing {
        val inCategory = filterTransactions(transactions, TransactionTypeFilter.ALL, categoryId)
        val listed = filterTransactions(inCategory, typeFilter)

        return TransactionListing(
            counts = TransactionTypeCounts(
                all = inCategory.size,
                expense = inCategory.count { it.transactionType == TransactionType.EXPENSE },
                income = inCategory.count { it.transactionType == TransactionType.INCOME }
            ),
            summary = summarizeTransactions(listed, typeFilter, isSearching),
            days = groupTransactionsByDay(listed)
        )
    }
}
