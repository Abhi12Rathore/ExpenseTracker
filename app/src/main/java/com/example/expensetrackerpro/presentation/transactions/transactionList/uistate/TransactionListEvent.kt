package com.example.expensetrackerpro.presentation.transactions.transactionList.uistate

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.utils.ExpenseFilter

sealed interface TransactionListEvent {
    data class FilterSelected(val filter: ExpenseFilter) : TransactionListEvent
    data class DeleteRequested(val transaction: Transaction) : TransactionListEvent
    data object DeleteConfirmed : TransactionListEvent
    data object DeleteDismissed : TransactionListEvent
}
