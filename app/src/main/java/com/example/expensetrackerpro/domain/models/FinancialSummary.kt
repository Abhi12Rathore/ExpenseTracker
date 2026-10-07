package com.example.expensetrackerpro.domain.models

data class FinancialSummary(
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val savingsRate: Float
)