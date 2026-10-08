package com.example.expensetrackerpro.domain.models

data class FinancialSummary(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val savingsRate: Float = 0f
)
