package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.model.TransactionWithCategory
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction

fun TransactionWithCategory.toDomain(): Transaction = Transaction(
    id = transaction.id,
    title = transaction.title,
    amount = Money(transaction.amountPaise),
    // Older versions saved the title into the note as well; that copy is not a real note
    note = transaction.note.takeUnless { it == transaction.title }.orEmpty(),
    dateMillis = transaction.dateMillis,
    categoryId = transaction.categoryId,
    categoryName = categoryName,
    transactionType = transaction.transactionType,
    categoryIcon = categoryIcon
)

/** The stored part only: the category's name and icon stay with the category. */
fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    title = title,
    amountPaise = amount.paise,
    note = note,
    dateMillis = dateMillis,
    categoryId = categoryId,
    transactionType = transactionType
)
