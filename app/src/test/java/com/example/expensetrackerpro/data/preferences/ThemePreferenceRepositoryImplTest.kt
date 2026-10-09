package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.expensetrackerpro.domain.models.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ThemePreferenceRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile("settings.preferences_pb").also { it.delete() }
        }

    @Test
    fun nothingSaved_followsSystem() = runTest {
        val repository = ThemePreferenceRepositoryImpl(dataStore())

        assertEquals(ThemeMode.SYSTEM, repository.getThemeMode().first())
    }

    @Test
    fun savedMode_isReadBack() = runTest {
        val repository = ThemePreferenceRepositoryImpl(dataStore())

        ThemeMode.entries.forEach { mode ->
            repository.setThemeMode(mode)

            assertEquals(mode, repository.getThemeMode().first())
        }
    }

    @Test
    fun unrecognisedSavedValue_followsSystem() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[ThemePreferenceRepositoryImpl.THEME_MODE] = "SEPIA" }

        assertEquals(ThemeMode.SYSTEM, ThemePreferenceRepositoryImpl(dataStore).getThemeMode().first())
    }
}
