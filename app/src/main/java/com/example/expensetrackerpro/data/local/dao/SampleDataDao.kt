package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity

/** Whole-table writes for the debug sample data. Nothing a user-facing feature should call. */
@Dao
abstract class SampleDataDao {

    @Query("SELECT * FROM categories")
    abstract suspend fun getCategories(): List<CategoryEntity>

    @Insert
    abstract suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert
    abstract suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    abstract suspend fun deleteAllTransactions()

    /**
     * Adds those of the built-in [categories] that are not stored, either under their key or,
     * for one that lost its key, under the same name and type. Returns all stored.
     */
    @Transaction
    open suspend fun addMissingCategories(categories: List<CategoryEntity>): List<CategoryEntity> {
        val stored = getCategories()
        val storedKeys = stored.mapNotNull { it.builtInKey }.toSet()
        val storedNames = stored.map { it.name to it.type }.toSet()
        insertCategories(
            categories.filter { it.builtInKey !in storedKeys && (it.name to it.type) !in storedNames }
        )

        return getCategories()
    }

    /** All or nothing: a failed insert leaves the old transactions in place. */
    @Transaction
    open suspend fun replaceAllTransactions(transactions: List<TransactionEntity>) {
        deleteAllTransactions()
        insertTransactions(transactions)
    }
}
