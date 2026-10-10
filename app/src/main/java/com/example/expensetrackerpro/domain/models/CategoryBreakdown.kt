package com.example.expensetrackerpro.domain.models

data class CategoryBreakdown(
    val categoryId: CategoryId,
    val categoryName: String,
    val icon: CategoryIcon,
    val amount: Money
)
