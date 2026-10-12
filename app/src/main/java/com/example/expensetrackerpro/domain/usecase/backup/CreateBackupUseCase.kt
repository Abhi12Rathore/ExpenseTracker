package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.onSuccess
import com.example.expensetrackerpro.domain.models.BuildInfo
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.repository.BackupInfoRepository
import com.example.expensetrackerpro.domain.repository.BackupRepository
import com.example.expensetrackerpro.domain.time.TimeProvider
import javax.inject.Inject

/** Saves every category and transaction as a backup file where the user chose. */
class CreateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val backupInfoRepository: BackupInfoRepository,
    private val timeProvider: TimeProvider,
    private val buildInfo: BuildInfo
) {

    /** A backup that was saved is also remembered as the last one made. */
    suspend operator fun invoke(destination: DocumentUri): Outcome<Unit> {
        val nowMillis = timeProvider.now().timeInMillis

        return backupRepository
            .writeBackup(destination, createdAtMillis = nowMillis, appVersion = buildInfo.versionName)
            .onSuccess { backupInfoRepository.setLastBackupMillis(nowMillis) }
    }
}
