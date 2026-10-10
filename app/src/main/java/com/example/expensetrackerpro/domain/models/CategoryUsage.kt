package com.example.expensetrackerpro.domain.models

/** How much a category is used: ever, and within the period it was asked for. */
data class CategoryUsage(
    val category: Category,
    // Every transaction filed under it, whenever it was made
    val totalCount: Int = 0,
    val periodCount: Int = 0,
    val periodAmount: Double = 0.0
)
