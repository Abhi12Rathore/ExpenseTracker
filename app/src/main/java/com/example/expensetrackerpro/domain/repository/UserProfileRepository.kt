package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {

    fun getProfile(): Flow<UserProfile>

    suspend fun setName(name: String)
}
