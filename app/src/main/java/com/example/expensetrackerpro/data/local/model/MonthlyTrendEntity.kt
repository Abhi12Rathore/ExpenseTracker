package com.example.expensetrackerpro.data.local.model

data class MonthlyTrendEntity(
    val year: Int,
    val month: Int,
    val income: Double,
    val expense: Double
)