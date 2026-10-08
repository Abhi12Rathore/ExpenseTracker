package com.example.expensetrackerpro.domain.models

data class Transaction(
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val note: String,
    val dateMillis: Long,
    val categoryId: Int,
    val categoryName: String,
    val transactionType: TransactionType
)
