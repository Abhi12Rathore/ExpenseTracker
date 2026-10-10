package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.UserProfile
import com.example.expensetrackerpro.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserProfileRepository(
    initialName: String = ""
) : UserProfileRepository {

    private val profile = MutableStateFlow(UserProfile(initialName))

    val current: UserProfile get() = profile.value

    override fun getProfile(): Flow<UserProfile> = profile

    override suspend fun setName(name: String) {
        profile.value = UserProfile(name)
    }
}
