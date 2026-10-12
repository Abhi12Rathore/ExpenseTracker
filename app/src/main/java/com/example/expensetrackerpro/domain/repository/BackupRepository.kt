package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.BackupFile
import com.example.expensetrackerpro.domain.models.DocumentUri

interface BackupRepository {

    /**
     * Writes every category and transaction to [destination] as a backup file. Fails with
     * FileWriteFailed when the file can't be written.
     */
    suspend fun writeBackup(destination: DocumentUri, createdAtMillis: Long, appVersion: String): Outcome<Unit>

    /**
     * Reads the backup file at [source] without checking what it says. Fails with FileReadFailed
     * when it can't be read, BackupInvalid when it is not a backup file, and BackupTooNew when a
     * newer version of the app wrote it.
     */
    suspend fun readBackup(source: DocumentUri): Outcome<BackupFile>

    /**
     * Replaces every category and transaction with those of [backup], all or nothing: when it
     * fails, what was stored before is still there.
     */
    suspend fun restore(backup: Backup): Outcome<Unit>
}
