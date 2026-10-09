package com.example.expensetrackerpro.presentation.transactions.transactionList.uistate

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent

sealed interface TransactionListEvent {
    data class Filter(val event: PeriodFilterEvent) : TransactionListEvent
    data object CategoryFilterCleared : TransactionListEvent
    data class SearchChanged(val query: String) : TransactionListEvent
    data class DeleteRequested(val transaction: Transaction) : TransactionListEvent
    data object DeleteConfirmed : TransactionListEvent
    data object DeleteDismissed : TransactionListEvent
}
