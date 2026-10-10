package com.example.expensetrackerpro.domain.repository

/** Fills the database for testing. Only debug builds offer it. */
interface SampleDataRepository {

    /**
     * Replaces every transaction with generated ones covering the last six months.
     * Returns how many were written, or null when the database rejected them.
     */
    suspend fun loadSampleData(): Int?
}
