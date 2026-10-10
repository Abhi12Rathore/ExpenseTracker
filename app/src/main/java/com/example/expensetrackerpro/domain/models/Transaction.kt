package com.example.expensetrackerpro.domain.models

data class Transaction(
    val id: TransactionId = TransactionId.UNSAVED,
    val title: String,
    val amount: Money,
    val note: String,
    val dateMillis: Long,
    val categoryId: CategoryId,
    // Read from the category, like the icon, never stored with the transaction
    val categoryName: String,
    val transactionType: TransactionType,
    val categoryIcon: CategoryIcon = CategoryIcon.DEFAULT
)
