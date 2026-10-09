package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.domain.models.Transaction

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    title = title,
    amount = amount,
    // Older versions saved the title into the note as well; that copy is not a real note
    note = note.takeUnless { it == title }.orEmpty(),
    dateMillis = dateMillis,
    categoryId = categoryId,
    categoryName = categoryName,
    transactionType = transactionType
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    title = title,
    amount = amount,
    note = note,
    dateMillis = dateMillis,
    categoryId = categoryId,
    categoryName = categoryName,
    transactionType = transactionType
)
