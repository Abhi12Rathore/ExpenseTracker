package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class AppLockRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppLockRepository {

    override fun getSettings(): Flow<AppLockSettings> =
        dataStore.data
            // An unreadable preferences file falls back to the defaults instead of failing the app
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences ->
                AppLockSettings(
                    enabled = preferences[ENABLED] ?: DEFAULTS.enabled,
                    timeout = preferences[TIMEOUT].toLockTimeout()
                )
            }
            .distinctUntilChanged()

    override suspend fun setSettings(settings: AppLockSettings) {
        dataStore.edit { preferences ->
            preferences[ENABLED] = settings.enabled
            preferences[TIMEOUT] = settings.timeout.name
        }
    }

    // Missing or unrecognised values use the default timeout
    private fun String?.toLockTimeout(): LockTimeout =
        LockTimeout.entries.firstOrNull { it.name == this } ?: DEFAULTS.timeout

    internal companion object {
        val ENABLED = booleanPreferencesKey("app_lock_enabled")
        val TIMEOUT = stringPreferencesKey("app_lock_timeout")
        private val DEFAULTS = AppLockSettings()
    }
}
