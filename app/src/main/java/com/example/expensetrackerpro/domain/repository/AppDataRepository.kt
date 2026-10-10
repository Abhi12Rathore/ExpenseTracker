package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.error.Outcome

interface AppDataRepository {

    /** Deletes every transaction and custom category, all or nothing. */
    suspend fun deleteAllData(): Outcome<Unit>
}
