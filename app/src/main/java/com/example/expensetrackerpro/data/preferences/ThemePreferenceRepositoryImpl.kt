package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.repository.ThemePreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class ThemePreferenceRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ThemePreferenceRepository {

    override fun getThemeMode(): Flow<ThemeMode> =
        dataStore.data
            // An unreadable preferences file falls back to the defaults instead of failing the app
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences -> preferences[THEME_MODE].toThemeMode() }
            .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[THEME_MODE] = mode.name }
    }

    // Missing or unrecognised values follow the system
    private fun String?.toThemeMode(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

    internal companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
