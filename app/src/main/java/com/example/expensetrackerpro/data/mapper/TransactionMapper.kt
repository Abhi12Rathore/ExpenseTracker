package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.model.TransactionWithCategory
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionId

fun TransactionWithCategory.toDomain(): Transaction = Transaction(
    id = TransactionId(transaction.id),
    title = transaction.title,
    amount = Money(transaction.amountPaise),
    // Older versions saved the title into the note as well; that copy is not a real note
    note = transaction.note.takeUnless { it == transaction.title }.orEmpty(),
    dateMillis = transaction.dateMillis,
    categoryId = CategoryId(transaction.categoryId),
    categoryName = categoryName,
    transactionType = transaction.transactionType,
    categoryIcon = categoryIcon
)

/** The stored part only: the category's name and icon stay with the category. */
fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id.value,
    title = title,
    amountPaise = amount.paise,
    note = note,
    dateMillis = dateMillis,
    categoryId = categoryId.value,
    transactionType = transactionType
)
