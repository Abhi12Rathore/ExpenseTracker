package com.example.expensetrackerpro.domain.models

data class CategoryBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val iconName: String,
    val amount: Double
)
