package com.example.expensetrackerpro.domain.usecase.profile

import com.example.expensetrackerpro.domain.models.UserProfile
import com.example.expensetrackerpro.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {

    operator fun invoke(): Flow<UserProfile> = userProfileRepository.getProfile()
}
