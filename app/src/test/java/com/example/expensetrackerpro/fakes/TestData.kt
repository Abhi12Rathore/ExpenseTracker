package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.domain.models.TransactionType

/** Whole rupees as [Money], to keep test amounts short. */
fun rupees(amount: Long): Money = Money.ofRupees(amount)

// Ids are given as plain numbers, to keep the tests short

fun transaction(
    id: Int = 1,
    amount: Money = rupees(100),
    type: TransactionType = TransactionType.EXPENSE,
    dateMillis: Long = 0L,
    note: String = "note",
    title: String = note
) = Transaction(
    id = TransactionId(id),
    title = title,
    amount = amount,
    note = note,
    dateMillis = dateMillis,
    categoryId = CategoryId(1),
    categoryName = "Food",
    transactionType = type
)

fun category(
    id: Int = 1,
    name: String = "Food",
    type: TransactionType = TransactionType.EXPENSE,
    icon: CategoryIcon = CategoryIcon.DEFAULT
) = Category(id = CategoryId(id), name = name, type = type, icon = icon)
