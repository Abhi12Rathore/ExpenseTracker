package com.example.expensetrackerpro.domain.models

data class CategoryBreakdown(
    val categoryName: String,
    val amount: Double,
    val percentage: Float
)