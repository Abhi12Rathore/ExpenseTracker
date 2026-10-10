package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.error.Outcome

/** Fills the database for testing. Only debug builds offer it. */
interface SampleDataRepository {

    /**
     * Replaces every transaction with generated ones covering the last six months.
     * Returns how many were written.
     */
    suspend fun loadSampleData(): Outcome<Int>
}
