package com.example.expensetrackerpro.data.backup

import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.model.StoredData
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.BackupCategoryRecord
import com.example.expensetrackerpro.domain.models.BackupDocument
import com.example.expensetrackerpro.domain.models.BackupTransactionRecord

/** What is stored, as the current version of the backup file. */
fun StoredData.toBackupDto(createdAtMillis: Long, appVersion: String, currency: AppCurrency): BackupDto = BackupDto(
    format = Backup.FORMAT,
    version = Backup.VERSION,
    createdAt = createdAtMillis,
    appVersion = appVersion,
    categories = categories.map { it.toBackupDto() },
    transactions = transactions.map { it.toBackupDto() },
    currency = currency.code
)

private fun CategoryEntity.toBackupDto(): BackupCategoryDto = BackupCategoryDto(
    id = id,
    name = name,
    type = type.name,
    // The key, as in the database: it stays the same when an icon's entry is renamed
    icon = icon.key,
    isCustom = isCustom,
    builtInKey = builtInKey?.key
)

// A transaction's own id is left out: a restore gives it a new one
private fun TransactionEntity.toBackupDto(): BackupTransactionDto = BackupTransactionDto(
    title = title,
    amountPaise = amountPaise,
    note = note,
    dateMillis = dateMillis,
    categoryId = categoryId,
    type = transactionType.name
)

/** The file's contents for the domain to check. Nothing is interpreted here. */
fun BackupDto.toDocument(): BackupDocument = BackupDocument(
    format = format,
    version = version,
    createdAtMillis = createdAt,
    appVersion = appVersion,
    categories = categories.map {
        BackupCategoryRecord(
            id = it.id,
            name = it.name,
            type = it.type,
            icon = it.icon,
            isCustom = it.isCustom,
            builtInKey = it.builtInKey
        )
    },
    transactions = transactions.map {
        BackupTransactionRecord(
            title = it.title,
            amountPaise = it.amountPaise,
            note = it.note,
            dateMillis = it.dateMillis,
            categoryId = it.categoryId,
            type = it.type
        )
    },
    currency = currency
)
