package com.example.expensetrackerpro.domain.usecase.data

import com.example.expensetrackerpro.domain.repository.AppDataRepository
import javax.inject.Inject

class DeleteAllDataUseCase @Inject constructor(
    private val appDataRepository: AppDataRepository
) {

    /** Deletes every transaction and custom category. Returns false on failure. */
    suspend operator fun invoke(): Boolean = appDataRepository.deleteAllData()
}
