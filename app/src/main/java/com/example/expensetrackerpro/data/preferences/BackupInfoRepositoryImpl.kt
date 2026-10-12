package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.expensetrackerpro.domain.repository.BackupInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class BackupInfoRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : BackupInfoRepository {

    override fun getLastBackupMillis(): Flow<Long?> =
        dataStore.data
            // An unreadable preferences file reads as no backup made, instead of failing the app
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences -> preferences[LAST_BACKUP_MILLIS] }
            .distinctUntilChanged()

    override suspend fun setLastBackupMillis(millis: Long) {
        dataStore.edit { preferences -> preferences[LAST_BACKUP_MILLIS] = millis }
    }

    internal companion object {
        val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
    }
}
