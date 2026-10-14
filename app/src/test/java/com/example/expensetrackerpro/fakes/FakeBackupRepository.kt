package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.BackupDocument
import com.example.expensetrackerpro.domain.models.BackupFile
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.repository.BackupRepository
import kotlinx.coroutines.CompletableDeferred

/** Keeps "files" in memory and records what it was asked to write and restore. */
class FakeBackupRepository : BackupRepository {

    /** What reading each URI gives. A URI that is not here can't be read. */
    val files = mutableMapOf<String, Outcome<BackupFile>>()

    /** Every backup written, oldest first. */
    val written = mutableListOf<WrittenBackup>()

    /** Every backup restored, oldest first. */
    val restored = mutableListOf<Backup>()

    var readCalls = 0
        private set

    /** While true every write fails, as a full or removed drive would make it. */
    var failWrites = false

    /** While true every restore fails, as a failing database would make it. */
    var failRestores = false

    /** When set, every write, read or restore waits for it to complete. */
    var gate: CompletableDeferred<Unit>? = null

    data class WrittenBackup(
        val destination: DocumentUri,
        val createdAtMillis: Long,
        val appVersion: String,
        val currency: AppCurrency = AppCurrency.INR
    )

    fun putFile(uri: String, document: BackupDocument, name: String = "backup.json") {
        files[uri] = Outcome.Success(BackupFile(name, document))
    }

    override suspend fun writeBackup(
        destination: DocumentUri,
        createdAtMillis: Long,
        appVersion: String,
        currency: AppCurrency
    ): Outcome<Unit> {
        gate?.await()
        if (failWrites) return Outcome.Failure(DomainError.FileWriteFailed)
        written += WrittenBackup(destination, createdAtMillis, appVersion, currency)
        return Outcome.Success(Unit)
    }

    override suspend fun readBackup(source: DocumentUri): Outcome<BackupFile> {
        readCalls++
        gate?.await()
        return files[source.uri] ?: Outcome.Failure(DomainError.FileReadFailed)
    }

    override suspend fun restore(backup: Backup): Outcome<Unit> {
        gate?.await()
        if (failRestores) return Outcome.Failure(DomainError.StorageFailure())
        restored += backup
        return Outcome.Success(Unit)
    }
}
