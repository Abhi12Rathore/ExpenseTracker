package com.example.expensetrackerpro.domain.usecase.sample

import com.example.expensetrackerpro.domain.repository.SampleDataRepository
import javax.inject.Inject

class ClearAllDataUseCase @Inject constructor(
    private val sampleDataRepository: SampleDataRepository
) {

    /** Deletes every transaction and custom category. Returns false on failure. */
    suspend operator fun invoke(): Boolean = sampleDataRepository.clearAllData()
}
