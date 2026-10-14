package com.example.expensetrackerpro.presentation.exportBackup.uistate

import androidx.annotation.StringRes
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.BackupPreview
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.LastBackup

/** The periods an export can cover, in the order they are offered. */
enum class ExportPeriod { MONTH, YEAR, ALL_TIME, CUSTOM }

data class ExportBackupUiState(
    val period: ExportPeriod = ExportPeriod.MONTH,
    // The days the period covers; everything for all time
    val dateRange: DateRange = DateRange.ALL,
    // How many transactions an export would write
    val transactionCount: Int = 0,
    // When the earliest and latest of them are dated; null when there are none
    val oldestMillis: Long? = null,
    val newestMillis: Long? = null,
    // The names the "Save as" picker opens with
    val suggestedFileName: String = "",
    val suggestedBackupFileName: String = "",
    val isExporting: Boolean = false,
    // How the last export ended, until the screen has shown it
    val message: ExportBackupMessage? = null,
    val backup: BackupUiState = BackupUiState()
) {
    /** True while a file is being written or read. One thing runs at a time. */
    val isBusy: Boolean get() = isExporting || backup.isBusy

    val canExport: Boolean get() = transactionCount > 0 && !isBusy

    /** What the screen has to say next: one message at a time, the export's first. */
    val pendingMessage: ExportBackupMessage? get() = message ?: backup.message
}

/** The Backup card and the sheet that confirms a restore. */
data class BackupUiState(
    // Null until a backup has been saved
    val lastBackup: LastBackup? = null,
    val isCreating: Boolean = false,
    // True while a picked file is read and checked
    val isReading: Boolean = false,
    val isRestoring: Boolean = false,
    // What the picked file holds; non-null while the confirmation sheet is showing
    val restorePreview: BackupPreview? = null,
    // Whether to show amounts in the backup's currency once restored; offered only when it differs
    val switchCurrency: Boolean = true,
    // How the last backup or restore ended, until the screen has shown it
    val message: ExportBackupMessage? = null
) {
    val isBusy: Boolean get() = isCreating || isReading || isRestoring
}

sealed interface ExportBackupMessage {
    /** [uri] is where the file was saved, for sharing it on. */
    data class Exported(val transactionCount: Int, val uri: String) : ExportBackupMessage
    data class BackupSaved(val uri: String) : ExportBackupMessage
    /** [switchedTo] is the currency amounts are now shown in, when the restore changed it. */
    data class Restored(val transactionCount: Int, val switchedTo: AppCurrency? = null) : ExportBackupMessage
    data class Failed(@StringRes val messageRes: Int) : ExportBackupMessage
}
