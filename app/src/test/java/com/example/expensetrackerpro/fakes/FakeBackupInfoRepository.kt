package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.repository.BackupInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBackupInfoRepository(
    initialMillis: Long? = null
) : BackupInfoRepository {

    private val lastBackupMillis = MutableStateFlow(initialMillis)

    val current: Long? get() = lastBackupMillis.value

    override fun getLastBackupMillis(): Flow<Long?> = lastBackupMillis

    override suspend fun setLastBackupMillis(millis: Long) {
        lastBackupMillis.value = millis
    }
}
