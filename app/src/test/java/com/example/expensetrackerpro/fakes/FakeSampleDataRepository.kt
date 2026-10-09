package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.repository.SampleDataRepository

/** Counts what it was asked to do instead of touching a database. */
class FakeSampleDataRepository(
    private val sampleCount: Int = 240
) : SampleDataRepository {

    var loadCalls = 0
        private set
    var clearCalls = 0
        private set

    /** While true both actions fail, as a failing database would make them. */
    var fail = false

    override suspend fun loadSampleData(): Int? {
        loadCalls++
        return if (fail) null else sampleCount
    }

    override suspend fun clearAllData(): Boolean {
        clearCalls++
        return !fail
    }
}
