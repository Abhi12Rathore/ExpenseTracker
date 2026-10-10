package com.example.expensetrackerpro.domain.models

data class FinancialSummary(
    val totalBalance: Money = Money.ZERO,
    val totalIncome: Money = Money.ZERO,
    val totalExpense: Money = Money.ZERO,
    val savingsRate: Float = 0f
)
