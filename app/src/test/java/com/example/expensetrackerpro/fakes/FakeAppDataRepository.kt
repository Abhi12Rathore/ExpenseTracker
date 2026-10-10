package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.repository.AppDataRepository

/** Counts what it was asked to do instead of touching a database. */
class FakeAppDataRepository : AppDataRepository {

    var deleteCalls = 0
        private set

    /** While true the delete fails with [failure], as a failing database would make it. */
    var fail = false

    var failure: DomainError = DomainError.StorageFailure()

    override suspend fun deleteAllData(): Outcome<Unit> {
        deleteCalls++
        return if (fail) Outcome.Failure(failure) else Outcome.Success(Unit)
    }
}
