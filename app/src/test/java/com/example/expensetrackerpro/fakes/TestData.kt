package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType

/** Whole rupees as [Money], to keep test amounts short. */
fun rupees(amount: Long): Money = Money.ofRupees(amount)

fun transaction(
    id: Int = 1,
    amount: Money = rupees(100),
    type: TransactionType = TransactionType.EXPENSE,
    dateMillis: Long = 0L,
    note: String = "note",
    title: String = note
) = Transaction(
    id = id,
    title = title,
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
    type: TransactionType = TransactionType.EXPENSE,
    icon: CategoryIcon = CategoryIcon.DEFAULT
) = Category(id = id, name = name, type = type, icon = icon)
