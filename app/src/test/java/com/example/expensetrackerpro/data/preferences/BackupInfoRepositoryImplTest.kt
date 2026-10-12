package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupInfoRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile("settings.preferences_pb").also { it.delete() }
        }

    @Test
    fun nothingSaved_isNoBackup() = runTest {
        assertNull(BackupInfoRepositoryImpl(dataStore()).getLastBackupMillis().first())
    }

    @Test
    fun theLastBackupTime_isReadBack_andReplacedByALaterOne() = runTest {
        val repository = BackupInfoRepositoryImpl(dataStore())

        repository.setLastBackupMillis(1_760_000_000_000L)
        assertEquals(1_760_000_000_000L, repository.getLastBackupMillis().first())

        repository.setLastBackupMillis(1_760_000_600_000L)
        assertEquals(1_760_000_600_000L, repository.getLastBackupMillis().first())
    }
}
