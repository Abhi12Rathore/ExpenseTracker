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

    /** Adds those of [categories] that no stored category matches by name and type; returns all stored. */
    @Transaction
    open suspend fun addMissingCategories(categories: List<CategoryEntity>): List<CategoryEntity> {
        val stored = getCategories().map { it.name to it.type }.toSet()
        insertCategories(categories.filter { (it.name to it.type) !in stored })

        return getCategories()
    }

    /** All or nothing: a failed insert leaves the old transactions in place. */
    @Transaction
    open suspend fun replaceAllTransactions(transactions: List<TransactionEntity>) {
        deleteAllTransactions()
        insertTransactions(transactions)
    }
}
