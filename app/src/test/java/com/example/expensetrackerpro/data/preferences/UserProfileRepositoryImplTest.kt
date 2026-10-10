package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.expensetrackerpro.domain.models.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserProfileRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile("settings.preferences_pb").also { it.delete() }
        }

    @Test
    fun nothingSaved_isABlankName() = runTest {
        val repository = UserProfileRepositoryImpl(dataStore())

        assertEquals(UserProfile(name = ""), repository.getProfile().first())
    }

    @Test
    fun savedName_isReadBack_andCanBeChangedAndCleared() = runTest {
        val repository = UserProfileRepositoryImpl(dataStore())

        repository.setName("Riya")
        assertEquals(UserProfile(name = "Riya"), repository.getProfile().first())

        repository.setName("Abhishek")
        assertEquals(UserProfile(name = "Abhishek"), repository.getProfile().first())

        repository.setName("")
        assertEquals(UserProfile(name = ""), repository.getProfile().first())
    }

    @Test
    fun theName_isKeptApartFromTheOtherSettings() = runTest {
        val dataStore = dataStore()
        val repository = UserProfileRepositoryImpl(dataStore)

        repository.setName("Riya")

        assertEquals(setOf(UserProfileRepositoryImpl.USER_NAME), dataStore.data.first().asMap().keys)
    }
}
