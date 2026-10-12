package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.BackupCategoryRecord
import com.example.expensetrackerpro.domain.models.BackupDocument
import com.example.expensetrackerpro.domain.models.BackupTransactionRecord

fun categoryRecord(
    id: Int = 1,
    name: String = "Food",
    type: String = "EXPENSE",
    icon: String = "restaurant",
    isCustom: Boolean = false
) = BackupCategoryRecord(id = id, name = name, type = type, icon = icon, isCustom = isCustom)

fun transactionRecord(
    title: String = "Lunch",
    amountPaise: Long = 250_00,
    note: String = "",
    dateMillis: Long = 0L,
    categoryId: Int = 1,
    type: String = "EXPENSE"
) = BackupTransactionRecord(
    title = title,
    amountPaise = amountPaise,
    note = note,
    dateMillis = dateMillis,
    categoryId = categoryId,
    type = type
)

/** A file that passes every check: two built-in categories, one custom, three transactions. */
fun backupDocument(
    format: String = Backup.FORMAT,
    version: Int = Backup.VERSION,
    createdAtMillis: Long = 1_000L,
    categories: List<BackupCategoryRecord> = listOf(
        categoryRecord(id = 1, name = "Food"),
        categoryRecord(id = 2, name = "Salary", type = "INCOME", icon = "payments"),
        categoryRecord(id = 7, name = "Pottery", icon = "pets", isCustom = true)
    ),
    transactions: List<BackupTransactionRecord> = listOf(
        transactionRecord(title = "Lunch", dateMillis = 300L),
        transactionRecord(title = "Pay", amountPaise = 85_000_00, dateMillis = 100L, categoryId = 2, type = "INCOME"),
        transactionRecord(title = "Clay", amountPaise = 5, dateMillis = 200L, categoryId = 7)
    )
) = BackupDocument(
    format = format,
    version = version,
    createdAtMillis = createdAtMillis,
    appVersion = "1.0",
    categories = categories,
    transactions = transactions
)
