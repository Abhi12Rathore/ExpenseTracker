package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.model.StoredData

/**
 * Whole-table reads and writes behind "Delete all data" and backup and restore. A restore replaces
 * every category, so what quick add remembered goes with them (ON DELETE CASCADE).
 */
@Dao
abstract class AppDataDao {

    @Query("SELECT * FROM categories ORDER BY id")
    abstract suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    abstract suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert
    abstract suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert
    abstract suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    abstract suspend fun deleteAllTransactions()

    @Query("DELETE FROM categories WHERE isCustom = 1")
    abstract suspend fun deleteCustomCategories()

    @Query("DELETE FROM categories")
    abstract suspend fun deleteAllCategories()

    @Query("DELETE FROM category_memory")
    abstract suspend fun deleteCategoryMemory()

    // Transactions go first: a category that still has some can't be deleted. The built-ins stay,
    // so what was remembered for them would too without its own delete
    @Transaction
    open suspend fun deleteAll() {
        deleteAllTransactions()
        deleteCustomCategories()
        deleteCategoryMemory()
    }

    /** Both tables read in one transaction, so every transaction's category is among the categories. */
    @Transaction
    open suspend fun snapshot(): StoredData = StoredData(getAllCategories(), getAllTransactions())

    /**
     * Replaces everything stored with [categories], under the ids they carry, and
     * [transactions]. All or nothing: when any step fails, what was stored before is still there.
     */
    @Transaction
    open suspend fun replaceAll(categories: List<CategoryEntity>, transactions: List<TransactionEntity>) {
        deleteAllTransactions()
        deleteAllCategories()
        insertCategories(categories)
        insertTransactions(transactions)
    }
}
