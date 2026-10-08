package com.example.expensetrackerpro.presentation.transactions.transactionList.uistate

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.utils.ExpenseFilter

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedFilter: ExpenseFilter = ExpenseFilter.ALL,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0, // Income - Expense
    // Non-null while the delete confirmation dialog is showing
    val transactionToDelete: Transaction? = null
)
