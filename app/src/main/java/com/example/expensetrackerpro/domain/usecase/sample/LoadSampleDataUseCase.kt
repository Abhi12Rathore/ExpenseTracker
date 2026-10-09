package com.example.expensetrackerpro.domain.usecase.sample

import com.example.expensetrackerpro.domain.repository.SampleDataRepository
import javax.inject.Inject

class LoadSampleDataUseCase @Inject constructor(
    private val sampleDataRepository: SampleDataRepository
) {

    /** Replaces every transaction with sample ones. Returns how many, or null on failure. */
    suspend operator fun invoke(): Int? = sampleDataRepository.loadSampleData()
}
