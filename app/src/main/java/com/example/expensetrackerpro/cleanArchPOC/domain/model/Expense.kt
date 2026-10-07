package com.example.expensetrackerpro.cleanArchPOC.domain.model

data class Expense(
    val id: String,
    val amount: Double,
    val category: String,
    val createdAtMillis: Long
)
