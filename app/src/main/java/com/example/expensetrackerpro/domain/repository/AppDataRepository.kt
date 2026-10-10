package com.example.expensetrackerpro.domain.repository

interface AppDataRepository {

    /** Deletes every transaction and custom category. Returns false when the database refused. */
    suspend fun deleteAllData(): Boolean
}
