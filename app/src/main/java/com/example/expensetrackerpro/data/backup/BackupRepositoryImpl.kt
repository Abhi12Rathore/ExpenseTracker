package com.example.expensetrackerpro.data.backup

import com.example.expensetrackerpro.data.files.DocumentFiles
import com.example.expensetrackerpro.data.local.dao.AppDataDao
import com.example.expensetrackerpro.data.mapper.storageOutcome
import com.example.expensetrackerpro.data.mapper.toEntity
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.BackupFile
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import java.io.IOException
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val appDataDao: AppDataDao,
    private val documentFiles: DocumentFiles
) : BackupRepository {

    override suspend fun writeBackup(
        destination: DocumentUri,
        createdAtMillis: Long,
        appVersion: String,
        currency: AppCurrency
    ): Outcome<Unit> = withContext(Dispatchers.IO) {
        val backup = appDataDao.snapshot().toBackupDto(createdAtMillis, appVersion, currency)

        try {
            // No byte order mark, unlike the CSV: JSON readers don't expect one
            documentFiles.write(destination, BackupJson.encodeToString(backup).toByteArray(Charsets.UTF_8))
            Outcome.Success(Unit)
        } catch (e: IOException) {
            Outcome.Failure(DomainError.FileWriteFailed)
        }
    }

    override suspend fun readBackup(source: DocumentUri): Outcome<BackupFile> =
        withContext(Dispatchers.IO) {
            val text = try {
                documentFiles.readText(source, MAX_BACKUP_BYTES)
            } catch (e: IOException) {
                return@withContext Outcome.Failure(DomainError.FileReadFailed)
            }
            // Far bigger than any backup: some other file was picked
                ?: return@withContext Outcome.Failure(DomainError.BackupInvalid)

            // Any file can be picked, so text that is not a backup is expected
            try {
                val header = BackupJson.decodeFromString<BackupHeaderDto>(text)
                // The rest of a newer file may be laid out in a way this version can't read
                if (header.format == Backup.FORMAT && header.version > Backup.VERSION) {
                    return@withContext Outcome.Failure(DomainError.BackupTooNew)
                }

                val document = BackupJson.decodeFromString<BackupDto>(text).toDocument()
                Outcome.Success(BackupFile(name = documentFiles.displayName(source).orEmpty(), document = document))
            } catch (e: SerializationException) {
                Outcome.Failure(DomainError.BackupInvalid)
            }
        }

    // The backup has been validated, so the database turning it down is a storage failure
    override suspend fun restore(backup: Backup): Outcome<Unit> =
        storageOutcome(foreignKeyError = DomainError.StorageFailure()) {
            appDataDao.replaceAll(
                categories = backup.categories.map { it.toEntity() },
                transactions = backup.transactions.map { it.toEntity() }
            )
        }

    private companion object {
        const val MAX_BACKUP_BYTES = 25 * 1024 * 1024
    }
}
