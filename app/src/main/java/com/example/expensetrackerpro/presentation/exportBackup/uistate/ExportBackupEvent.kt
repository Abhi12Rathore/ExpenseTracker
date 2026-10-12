package com.example.expensetrackerpro.presentation.exportBackup.uistate

sealed interface ExportBackupEvent {
    /** Month and Year are the ones we are in now. Custom is chosen with [CustomRangeSelected]. */
    data class PeriodSelected(val period: ExportPeriod) : ExportBackupEvent
    // Both are the picker's values: the chosen dates as midnight UTC
    data class CustomRangeSelected(val startUtcMillis: Long, val endUtcMillis: Long) : ExportBackupEvent
    /** The user chose where to save the file. Backing out of the picker sends nothing. */
    data class DestinationPicked(val uri: String) : ExportBackupEvent
    data object MessageShown : ExportBackupEvent
}
