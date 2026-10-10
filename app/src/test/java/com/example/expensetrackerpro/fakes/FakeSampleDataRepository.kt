package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.repository.SampleDataRepository

/** Counts what it was asked to do instead of touching a database. */
class FakeSampleDataRepository(
    private val sampleCount: Int = 240
) : SampleDataRepository {

    var loadCalls = 0
        private set

    /** While true loading fails with [failure], as a failing database would make it. */
    var fail = false

    var failure: DomainError = DomainError.StorageFailure()

    override suspend fun loadSampleData(): Outcome<Int> {
        loadCalls++
        return if (fail) Outcome.Failure(failure) else Outcome.Success(sampleCount)
    }
}
