package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.model.TransactionWithCategory
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // --- Transaction CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Reads join the category for its name and icon, which are not stored on the transaction

    @Query("SELECT t.*, c.name AS categoryName, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId WHERE t.id = :id")
    suspend fun getTransactionById(id: Int): TransactionWithCategory?

    @Query("SELECT t.*, c.name AS categoryName, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId ORDER BY t.dateMillis DESC, t.id DESC LIMIT 1")
    suspend fun getLatestTransaction(): TransactionWithCategory?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int

    @Query("SELECT t.*, c.name AS categoryName, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId ORDER BY t.dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionWithCategory>>

    @Query("SELECT t.*, c.name AS categoryName, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId WHERE t.transactionType = :type ORDER BY t.dateMillis DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionWithCategory>>

    @Query(
        """
SELECT t.*, c.name AS categoryName, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId
WHERE t.dateMillis >= :startMillis AND t.dateMillis < :endMillis
ORDER BY t.dateMillis DESC
"""
    )
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionWithCategory>>

    // :query must already have %, _ and \ escaped with a backslash
    @Query(
        """
SELECT t.*, c.name AS categoryName, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId
WHERE t.dateMillis >= :startMillis AND t.dateMillis < :endMillis
AND (
    t.title LIKE '%' || :query || '%' ESCAPE '\'
    OR t.note LIKE '%' || :query || '%' ESCAPE '\'
    OR c.name LIKE '%' || :query || '%' ESCAPE '\'
)
ORDER BY t.dateMillis DESC
"""
    )
    fun searchTransactionsBetween(
        startMillis: Long,
        endMillis: Long,
        query: String
    ): Flow<List<TransactionWithCategory>>
}
