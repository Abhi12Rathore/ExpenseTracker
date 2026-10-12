package com.example.expensetrackerpro.presentation.exportBackup.uistate

import androidx.annotation.StringRes
import com.example.expensetrackerpro.domain.models.DateRange

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
    // The name the "Save as" picker opens with
    val suggestedFileName: String = "",
    val isExporting: Boolean = false,
    // How the last export ended, until the screen has shown it
    val message: ExportBackupMessage? = null
) {
    val canExport: Boolean get() = transactionCount > 0 && !isExporting
}

sealed interface ExportBackupMessage {
    data class Exported(val transactionCount: Int) : ExportBackupMessage
    data class Failed(@StringRes val messageRes: Int) : ExportBackupMessage
}
