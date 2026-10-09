package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.LockTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import java.io.IOException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppLockRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile("settings.preferences_pb").also { it.delete() }
        }

    @Test
    fun nothingSaved_isOffWithThirtySeconds() = runTest {
        val repository = AppLockRepositoryImpl(dataStore())

        assertEquals(
            AppLockSettings(enabled = false, timeout = LockTimeout.SECONDS_30),
            repository.getSettings().first()
        )
    }

    @Test
    fun savedSettings_areReadBack() = runTest {
        val repository = AppLockRepositoryImpl(dataStore())

        LockTimeout.entries.forEach { timeout ->
            listOf(true, false).forEach { enabled ->
                val settings = AppLockSettings(enabled = enabled, timeout = timeout)

                repository.setSettings(settings)

                assertEquals(settings, repository.getSettings().first())
            }
        }
    }

    @Test
    fun unrecognisedSavedTimeout_usesThirtySecondsAndKeepsTheSwitch() = runTest {
        val dataStore = dataStore()
        dataStore.edit {
            it[AppLockRepositoryImpl.ENABLED] = true
            it[AppLockRepositoryImpl.TIMEOUT] = "HOUR_1"
        }

        assertEquals(
            AppLockSettings(enabled = true, timeout = LockTimeout.SECONDS_30),
            AppLockRepositoryImpl(dataStore).getSettings().first()
        )
    }

    @Test
    fun unreadablePreferences_failClosedWithTheLockOn() = runTest {
        val unreadable = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt file") }

            override suspend fun updateData(
                transform: suspend (t: Preferences) -> Preferences
            ): Preferences = throw IOException("corrupt file")
        }

        assertEquals(
            AppLockSettings(enabled = true, timeout = LockTimeout.SECONDS_30),
            AppLockRepositoryImpl(unreadable).getSettings().first()
        )
    }

    @Test
    fun failureThatIsNotAnIoError_isNotSwallowed() = runTest {
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IllegalStateException("bug") }

            override suspend fun updateData(
                transform: suspend (t: Preferences) -> Preferences
            ): Preferences = throw IllegalStateException("bug")
        }

        assertThrows(IllegalStateException::class.java) {
            runBlocking { AppLockRepositoryImpl(broken).getSettings().first() }
        }
    }

    @Test
    fun sharesTheFileWithTheThemeWithoutOverwritingIt() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[ThemePreferenceRepositoryImpl.THEME_MODE] = "DARK" }

        AppLockRepositoryImpl(dataStore).setSettings(AppLockSettings(enabled = true))

        assertEquals("DARK", dataStore.data.first()[ThemePreferenceRepositoryImpl.THEME_MODE])
    }
}
