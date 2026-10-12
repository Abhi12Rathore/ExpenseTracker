package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.map
import com.example.expensetrackerpro.domain.models.BackupPreview
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** What restoring a picked file would bring back, and what it would replace. */
class PreviewBackupUseCase @Inject constructor(
    private val readBackup: ReadBackupUseCase,
    private val getTransactions: GetTransactionsUseCase
) {

    /** Fails, as reading the file does, unless it is a backup that can be restored. */
    suspend operator fun invoke(source: DocumentUri): Outcome<BackupPreview> =
        readBackup(source).map { (fileName, backup) ->
            BackupPreview(
                fileName = fileName,
                createdAtMillis = backup.createdAtMillis,
                transactionCount = backup.transactions.size,
                customCategoryCount = backup.categories.count { it.isCustom },
                oldestMillis = backup.transactions.minOfOrNull { it.dateMillis },
                newestMillis = backup.transactions.maxOfOrNull { it.dateMillis },
                currentTransactionCount = getTransactions(DateRange.ALL).first().size
            )
        }
}
