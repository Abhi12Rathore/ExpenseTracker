package com.example.expensetrackerpro.presentation.exportBackup.uistate

sealed interface ExportBackupEvent {
    /** Month and Year are the ones we are in now. Custom is chosen with [CustomRangeSelected]. */
    data class PeriodSelected(val period: ExportPeriod) : ExportBackupEvent
    // Both are the picker's values: the chosen dates as midnight UTC
    data class CustomRangeSelected(val startUtcMillis: Long, val endUtcMillis: Long) : ExportBackupEvent
    /** The user chose where to save the CSV. Backing out of a picker sends nothing. */
    data class DestinationPicked(val uri: String) : ExportBackupEvent

    data class BackupDestinationPicked(val uri: String) : ExportBackupEvent
    /** A file to restore from was chosen. It is checked, then a sheet asks before anything is replaced. */
    data class RestoreFilePicked(val uri: String) : ExportBackupEvent
    data object RestoreConfirmed : ExportBackupEvent
    data object RestoreDismissed : ExportBackupEvent

    data object MessageShown : ExportBackupEvent
}
