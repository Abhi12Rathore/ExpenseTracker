package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.map
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.repository.BackupRepository
import javax.inject.Inject

/** Replaces everything in the app with what a backup file holds. */
class RestoreBackupUseCase @Inject constructor(
    private val readBackup: ReadBackupUseCase,
    private val backupRepository: BackupRepository
) {

    /**
     * The file is read and checked again here, as it may have changed since it was previewed.
     * Returns how many transactions were restored. On any failure nothing stored has changed.
     */
    suspend operator fun invoke(source: DocumentUri): Outcome<Int> =
        when (val read = readBackup(source)) {
            is Outcome.Failure -> read
            is Outcome.Success -> {
                val backup = read.value.second
                backupRepository.restore(backup).map { backup.transactions.size }
            }
        }
}
