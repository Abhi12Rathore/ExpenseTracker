package com.example.expensetrackerpro.cleanArchPOC.data.remote

data class ExpenseDto(
    val id: String,
    val amount: Double,
    val category: String,
    val createdAtMillis: Long
)

