package com.example.expensetrackerpro.domain.models

data class MonthlyTrend(
    val year: Int,
    val month: Int,
    val income: Double,
    val expense: Double
)
