package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.repository.AppDataRepository

/** Counts what it was asked to do instead of touching a database. */
class FakeAppDataRepository : AppDataRepository {

    var deleteCalls = 0
        private set

    /** While true the delete fails, as a failing database would make it. */
    var fail = false

    override suspend fun deleteAllData(): Boolean {
        deleteCalls++
        return !fail
    }
}
