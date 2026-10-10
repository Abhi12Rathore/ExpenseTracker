package com.example.expensetrackerpro.domain.usecase.profile

import com.example.expensetrackerpro.domain.repository.UserProfileRepository
import javax.inject.Inject

class SetUserNameUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) {

    /** Stores [name] without the space around it, cut at [NAME_MAX_LENGTH]. Blank clears it. */
    suspend operator fun invoke(name: String) {
        userProfileRepository.setName(name.trim().take(NAME_MAX_LENGTH).trim())
    }

    companion object {
        const val NAME_MAX_LENGTH = 30
    }
}
