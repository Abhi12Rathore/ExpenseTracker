package com.example.expensetrackerpro.presentation.transactions.transactionList.uistate

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent

sealed interface TransactionListEvent {
    data class Filter(val event: PeriodFilterEvent) : TransactionListEvent
    data class TypeFilterSelected(val typeFilter: TransactionTypeFilter) : TransactionListEvent
    data object CategoryFilterCleared : TransactionListEvent
    data object SearchOpened : TransactionListEvent
    // Closing also clears what was typed
    data object SearchClosed : TransactionListEvent
    data class SearchChanged(val query: String) : TransactionListEvent
    /** Widens the period to everything, keeping the search. */
    data object SearchAllTime : TransactionListEvent
    /** A row was swiped away. It is deleted at once; the app offers the Undo. */
    data class DeleteSwiped(val transaction: Transaction) : TransactionListEvent
}
