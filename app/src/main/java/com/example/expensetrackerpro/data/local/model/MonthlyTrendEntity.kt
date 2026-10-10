package com.example.expensetrackerpro.data.local.model

data class MonthlyTrendEntity(
    val year: Int,
    val month: Int,
    val incomePaise: Long,
    val expensePaise: Long
)