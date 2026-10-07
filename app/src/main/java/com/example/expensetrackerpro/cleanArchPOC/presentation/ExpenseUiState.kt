package com.example.expensetrackerpro.cleanArchPOC.presentation

import com.example.expensetrackerpro.cleanArchPOC.domain.model.Expense
import com.example.expensetrackerpro.cleanArchPOC.presentation.error.ExpenseUiError

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val isRefreshing: Boolean = false,
    val refreshError: ExpenseUiError? = null
)