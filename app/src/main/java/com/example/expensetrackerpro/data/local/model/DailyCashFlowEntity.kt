package com.example.expensetrackerpro.data.local.model

data class DailyCashFlowEntity(
    val dateMillis: Long,
    val income: Double,
    val expense: Double
)