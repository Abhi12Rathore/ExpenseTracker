package com.example.expensetrackerpro.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How backup files are written and read. A later version of the app may add fields, which this one leaves unread. */
internal val BackupJson = Json { ignoreUnknownKeys = true }

// The backup file, field for field. Renaming anything here changes the file format.

@Serializable
data class BackupDto(
    val format: String,
    val version: Int,
    val createdAt: Long,
    val appVersion: String,
    val categories: List<BackupCategoryDto>,
    val transactions: List<BackupTransactionDto>
)

/** [type] is the TransactionType's name and [icon] the CategoryIcon's key. */
@Serializable
data class BackupCategoryDto(
    val id: Int,
    val name: String,
    val type: String,
    val icon: String,
    val isCustom: Boolean
)

@Serializable
data class BackupTransactionDto(
    val title: String,
    val amountPaise: Long,
    val note: String,
    val dateMillis: Long,
    val categoryId: Int,
    val type: String
)

/** The two fields every version of the file starts from, read before the rest is trusted. */
@Serializable
data class BackupHeaderDto(
    val format: String,
    val version: Int
)
