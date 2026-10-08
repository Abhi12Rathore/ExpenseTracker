package com.example.expensetrackerpro.domain.models

data class CashFlowPoint(
    val dateMillis: Long,
    val income: Double,
    val expense: Double
)
