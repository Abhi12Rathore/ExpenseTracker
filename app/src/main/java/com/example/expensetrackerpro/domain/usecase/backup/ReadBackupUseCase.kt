package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.repository.BackupRepository
import javax.inject.Inject

/** Reads a file the user picked and checks all of it. Nothing stored is touched. */
class ReadBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val validateBackup: ValidateBackupUseCase
) {

    /** The file's name with the backup it holds. */
    suspend operator fun invoke(source: DocumentUri): Outcome<Pair<String, Backup>> =
        when (val file = backupRepository.readBackup(source)) {
            is Outcome.Failure -> file
            is Outcome.Success -> when (val backup = validateBackup(file.value.document)) {
                is Outcome.Failure -> backup
                is Outcome.Success -> Outcome.Success(file.value.name to backup.value)
            }
        }
}
