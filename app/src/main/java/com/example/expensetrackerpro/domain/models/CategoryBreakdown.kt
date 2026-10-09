package com.example.expensetrackerpro.domain.models

data class CategoryBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val icon: CategoryIcon,
    val amount: Double
)
