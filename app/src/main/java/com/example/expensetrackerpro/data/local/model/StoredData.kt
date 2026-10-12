package com.example.expensetrackerpro.data.local.model

import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity

/** Every category and transaction, as they were stored at one moment. */
data class StoredData(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>
)
