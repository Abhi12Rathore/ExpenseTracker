package com.example.expensetrackerpro.data.local.model

data class DailyCashFlowEntity(
    val dateMillis: Long,
    val incomePaise: Long,
    val expensePaise: Long
)