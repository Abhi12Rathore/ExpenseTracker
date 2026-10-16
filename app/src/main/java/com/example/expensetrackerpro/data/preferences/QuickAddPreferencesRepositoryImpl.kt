package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.example.expensetrackerpro.domain.repository.QuickAddPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class QuickAddPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : QuickAddPreferencesRepository {

    override fun hasUsedQuickAdd(): Flow<Boolean> =
        dataStore.data
            // An unreadable preferences file reads as never used, which only shows the examples again
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences -> preferences[USED] ?: false }
            .distinctUntilChanged()

    override suspend fun setUsedQuickAdd() {
        dataStore.edit { preferences -> preferences[USED] = true }
    }

    internal companion object {
        val USED = booleanPreferencesKey("quick_add_used")
    }
}
