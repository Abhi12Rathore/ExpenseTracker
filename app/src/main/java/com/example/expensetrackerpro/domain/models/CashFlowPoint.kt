package com.example.expensetrackerpro.domain.models

data class CashFlowPoint(
    val dateMillis: Long,
    val income: Money,
    val expense: Money
)
