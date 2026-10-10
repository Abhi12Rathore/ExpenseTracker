package com.example.expensetrackerpro.domain.usecase.sample

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.repository.SampleDataRepository
import javax.inject.Inject

class LoadSampleDataUseCase @Inject constructor(
    private val sampleDataRepository: SampleDataRepository
) {

    /** Replaces every transaction with sample ones. Returns how many. */
    suspend operator fun invoke(): Outcome<Int> = sampleDataRepository.loadSampleData()
}
