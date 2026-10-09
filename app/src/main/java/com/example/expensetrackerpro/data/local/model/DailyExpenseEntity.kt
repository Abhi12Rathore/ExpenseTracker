package com.example.expensetrackerpro.data.local.model

data class DailyExpenseEntity(
    val dayMillis: Long,
    val amount: Double,
    val transactionCount: Int
)
