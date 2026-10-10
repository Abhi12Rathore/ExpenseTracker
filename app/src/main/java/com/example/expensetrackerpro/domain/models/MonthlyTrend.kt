package com.example.expensetrackerpro.domain.models

data class MonthlyTrend(
    val year: Int,
    val month: Int,
    val income: Money,
    val expense: Money
)
