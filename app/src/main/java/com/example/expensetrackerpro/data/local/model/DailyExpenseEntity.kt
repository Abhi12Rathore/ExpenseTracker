package com.example.expensetrackerpro.data.local.model

data class DailyExpenseEntity(
    val dayMillis: Long,
    val amountPaise: Long,
    val transactionCount: Int
)
