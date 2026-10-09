package com.example.expensetrackerpro.presentation.transactions.transactionList.uistate

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.navigation.CategoryFilter

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val filter: PeriodFilterState = PeriodFilterState(),
    // Non-null when only one category's transactions are listed
    val categoryFilter: CategoryFilter? = null,
    // As typed; the list follows a moment later
    val searchQuery: String = "",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0, // Income - Expense
    // Non-null while the delete confirmation dialog is showing
    val transactionToDelete: Transaction? = null
)
