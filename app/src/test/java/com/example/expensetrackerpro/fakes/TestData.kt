package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType

fun transaction(
    id: Int = 1,
    amount: Double = 100.0,
    type: TransactionType = TransactionType.EXPENSE,
    dateMillis: Long = 0L,
    note: String = "note"
) = Transaction(
    id = id,
    title = note,
    amount = amount,
    note = note,
    dateMillis = dateMillis,
    categoryId = 1,
    categoryName = "Food",
    transactionType = type
)

fun category(
    id: Int = 1,
    name: String = "Food",
    type: TransactionType = TransactionType.EXPENSE
) = Category(id = id, name = name, type = type, iconName = name)
