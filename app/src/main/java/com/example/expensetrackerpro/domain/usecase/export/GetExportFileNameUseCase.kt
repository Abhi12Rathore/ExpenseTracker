package com.example.expensetrackerpro.domain.usecase.export

import com.example.expensetrackerpro.domain.models.ExportFile
import com.example.expensetrackerpro.domain.time.TimeProvider
import com.example.expensetrackerpro.utils.DateFormatter
import javax.inject.Inject

/**
 * The name a file is offered under, dated today: `expenses_2026-10-11.csv` or
 * `expensetracker-backup-2026-10-11.json`.
 */
class GetExportFileNameUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    operator fun invoke(file: ExportFile): String {
        val today = DateFormatter.formatIsoDate(timeProvider.now().timeInMillis)

        return when (file) {
            ExportFile.TRANSACTIONS_CSV -> "expenses_$today.csv"
            ExportFile.BACKUP -> "expensetracker-backup-$today.json"
        }
    }
}
