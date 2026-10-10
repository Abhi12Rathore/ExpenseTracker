package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** Whole-table deletes behind "Delete all data". */
@Dao
abstract class AppDataDao {

    @Query("DELETE FROM transactions")
    abstract suspend fun deleteAllTransactions()

    @Query("DELETE FROM categories WHERE isCustom = 1")
    abstract suspend fun deleteCustomCategories()

    // Transactions go first: a category that still has some can't be deleted
    @Transaction
    open suspend fun deleteAll() {
        deleteAllTransactions()
        deleteCustomCategories()
    }
}
