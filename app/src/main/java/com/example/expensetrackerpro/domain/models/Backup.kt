package com.example.expensetrackerpro.domain.models

/**
 * Everything a backup holds, checked and ready to be restored: the categories with the ids the
 * transactions refer to, and the transactions. Settings such as the app lock are never part of it.
 */
data class Backup(
    val createdAtMillis: Long,
    // The app version that wrote the file, e.g. 1.0
    val appVersion: String,
    val categories: List<Category>,
    // Not stored yet, so none has an id of its own
    val transactions: List<Transaction>
) {
    companion object {
        /** What every backup file names itself as. */
        const val FORMAT = "expensetracker-backup"

        /** The newest file version this app reads and the one it writes. */
        const val VERSION = 1
    }
}

/** A backup file as it was read. Nothing in it has been checked yet. */
data class BackupDocument(
    val format: String,
    val version: Int,
    val createdAtMillis: Long,
    val appVersion: String,
    val categories: List<BackupCategoryRecord>,
    val transactions: List<BackupTransactionRecord>
)

/** [type] and [icon] are as written in the file, which may not be values this app knows. */
data class BackupCategoryRecord(
    val id: Int,
    val name: String,
    val type: String,
    val icon: String,
    val isCustom: Boolean
)

data class BackupTransactionRecord(
    val title: String,
    val amountPaise: Long,
    val note: String,
    val dateMillis: Long,
    val categoryId: Int,
    val type: String
)

/** A file that was picked to restore from, under the name the user knows it by. */
data class BackupFile(
    val name: String,
    val document: BackupDocument
)

/** What the user is shown before deciding to restore a backup. */
data class BackupPreview(
    val fileName: String,
    val createdAtMillis: Long,
    val transactionCount: Int,
    val customCategoryCount: Int,
    // When the earliest and latest transactions in the backup are dated; null when it has none
    val oldestMillis: Long?,
    val newestMillis: Long?,
    // How many transactions the app holds now, all of which a restore replaces
    val currentTransactionCount: Int
)

/** When the last backup was saved, and how that day is named relative to today. */
data class LastBackup(
    val millis: Long,
    val day: DayLabel
)
