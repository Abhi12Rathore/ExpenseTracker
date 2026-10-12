package com.example.expensetrackerpro.domain.repository

import kotlinx.coroutines.flow.Flow

interface BackupInfoRepository {

    /** When the last backup was saved, or null when none has been. */
    fun getLastBackupMillis(): Flow<Long?>

    suspend fun setLastBackupMillis(millis: Long)
}
