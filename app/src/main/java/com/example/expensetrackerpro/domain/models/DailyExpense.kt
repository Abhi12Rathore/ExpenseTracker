package com.example.expensetrackerpro.domain.models

/** What was spent on one local day. [dateMillis] is a moment within that day. */
data class DailyExpense(
    val dateMillis: Long,
    val amount: Money,
    val transactionCount: Int
)
