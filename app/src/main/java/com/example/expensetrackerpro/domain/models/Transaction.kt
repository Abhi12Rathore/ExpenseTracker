package com.example.expensetrackerpro.domain.models

data class Transaction(
    val id: Int = 0,
    val title: String,
    val amount: Money,
    val note: String,
    val dateMillis: Long,
    val categoryId: Int,
    // Read from the category, like the icon, never stored with the transaction
    val categoryName: String,
    val transactionType: TransactionType,
    val categoryIcon: CategoryIcon = CategoryIcon.DEFAULT
)
