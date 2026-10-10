package com.example.expensetrackerpro.domain.usecase.data

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.repository.AppDataRepository
import javax.inject.Inject

class DeleteAllDataUseCase @Inject constructor(
    private val appDataRepository: AppDataRepository
) {

    /** Deletes every transaction and custom category. */
    suspend operator fun invoke(): Outcome<Unit> = appDataRepository.deleteAllData()
}
