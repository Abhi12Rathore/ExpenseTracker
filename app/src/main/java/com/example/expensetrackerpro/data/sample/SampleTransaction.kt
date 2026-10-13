package com.example.expensetrackerpro.data.sample

import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType

/** A generated transaction, naming its built-in category rather than pointing at a stored one. */
data class SampleTransaction(
    val title: String,
    val amount: Money,
    val note: String,
    val dateMillis: Long,
    val category: BuiltInCategory,
    val type: TransactionType
)
