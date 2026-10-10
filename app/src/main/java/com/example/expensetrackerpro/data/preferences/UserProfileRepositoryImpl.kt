package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.expensetrackerpro.domain.models.UserProfile
import com.example.expensetrackerpro.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserProfileRepository {

    override fun getProfile(): Flow<UserProfile> =
        dataStore.data
            // An unreadable preferences file falls back to the defaults instead of failing the app
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences -> UserProfile(name = preferences[USER_NAME].orEmpty()) }
            .distinctUntilChanged()

    override suspend fun setName(name: String) {
        dataStore.edit { preferences -> preferences[USER_NAME] = name }
    }

    internal companion object {
        val USER_NAME = stringPreferencesKey("user_name")
    }
}
