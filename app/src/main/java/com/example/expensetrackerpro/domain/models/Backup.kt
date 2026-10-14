package com.example.expensetrackerpro.domain.models

import com.example.expensetrackerpro.domain.currency.AppCurrency

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
    val transactions: List<Transaction>,
    // The currency the amounts were shown in; null in files written before it was noted, or one this app doesn't offer
    val currency: AppCurrency? = null
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
    val transactions: List<BackupTransactionRecord>,
    // An ISO currency code, as written in the file
    val currency: String? = null
)

/** [type] and [icon] are as written in the file, which may not be values this app knows. */
data class BackupCategoryRecord(
    val id: Int,
    val name: String,
    val type: String,
    val icon: String,
    val isCustom: Boolean,
    // The BuiltInCategory's key; null for a custom or renamed category, and in files written before it existed
    val builtInKey: String? = null
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
    val currentTransactionCount: Int,
    // The currency the backup's amounts were shown in, when the file says and it isn't the one shown now
    val differentCurrency: AppCurrency? = null,
    val currentCurrency: AppCurrency = AppCurrency.INR
)

/** How a restore went: how many transactions came back, and the currency switched to, if it was. */
data class RestoredBackup(
    val transactionCount: Int,
    val switchedTo: AppCurrency? = null
)

/** When the last backup was saved, and how that day is named relative to today. */
data class LastBackup(
    val millis: Long,
    val day: DayLabel
)
