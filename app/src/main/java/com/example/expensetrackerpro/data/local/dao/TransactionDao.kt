package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.data.local.model.TransactionWithCategory
import com.example.expensetrackerpro.data.local.model.DailyCashFlowEntity
import com.example.expensetrackerpro.data.local.model.DailyExpenseEntity
import kotlinx.coroutines.flow.Flow
import com.example.expensetrackerpro.data.local.model.CategoryExpenseEntity
import com.example.expensetrackerpro.data.local.model.MonthlyTrendEntity

@Dao
interface TransactionDao {

    // --- Transaction CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Reads join the category for its icon, which is not stored on the transaction

    @Query("SELECT t.*, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId WHERE t.id = :id")
    suspend fun getTransactionById(id: Int): TransactionWithCategory?

    // --- Category CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // --- Existing Queries (Flows) ---
    @Query("SELECT t.*, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId ORDER BY t.dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionWithCategory>>

    @Query("SELECT t.*, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId WHERE t.transactionType = :type ORDER BY t.dateMillis DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionWithCategory>>

    @Query(
        """
SELECT t.*, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId
WHERE t.dateMillis >= :startMillis AND t.dateMillis < :endMillis
ORDER BY t.dateMillis DESC
"""
    )
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionWithCategory>>

    // :query must already have %, _ and \ escaped with a backslash. The category is matched by
    // its current name, not the copy stored on the transaction.
    @Query(
        """
SELECT t.*, c.icon AS categoryIcon FROM transactions t JOIN categories c ON c.id = t.categoryId
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

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type")
    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    // --- Financial Stats ---
    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'INCOME' AND dateMillis >= :startMillis")
    fun getMonthlyIncome(startMillis: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'EXPENSE' AND dateMillis >= :startMillis")
    fun getMonthlyExpense(startMillis: Long): Flow<Double?>


    @Query(
        """
SELECT COALESCE(SUM(amount),0)
FROM transactions
WHERE transactionType = 'INCOME'
AND dateMillis BETWEEN :startDate AND :endDate
"""
    )
    suspend fun getTotalIncome(
        startDate: Long,
        endDate: Long
    ): Double

    @Query(
        """
SELECT COALESCE(SUM(amount),0)
FROM transactions
WHERE transactionType = 'EXPENSE'
AND dateMillis BETWEEN :startDate AND :endDate
"""
    )
    suspend fun getTotalExpense(
        startDate: Long,
        endDate: Long
    ): Double

    @Query(
        """
SELECT 
 c.id AS categoryId,
 c.name AS categoryName,
 c.icon AS icon,
 SUM(t.amount) AS totalAmount

FROM transactions t
JOIN categories c
ON t.categoryId = c.id

WHERE t.transactionType = 'EXPENSE'
AND t.dateMillis >= :startMillis AND t.dateMillis < :endMillis

GROUP BY c.id

ORDER BY totalAmount DESC

"""
    )
    fun getCategoryExpense(startMillis: Long, endMillis: Long):
            Flow<List<CategoryExpenseEntity>>


    @Query("""
SELECT

dateMillis,

SUM(
CASE 
WHEN transactionType='INCOME' THEN amount
ELSE 0
END
) income,


SUM(
CASE 
WHEN transactionType='EXPENSE' THEN amount
ELSE 0
END
) expense


FROM transactions

WHERE dateMillis >= :startMillis AND dateMillis < :endMillis

GROUP BY date(dateMillis / 1000,'unixepoch','localtime')

ORDER BY dateMillis
""")
    fun getDailyCashFlow(startMillis: Long, endMillis: Long): Flow<List<DailyCashFlowEntity>>

    // Only days that have an expense come back; dayMillis is that day's first expense
    @Query(
        """
SELECT

MIN(dateMillis) AS dayMillis,

SUM(amount) AS amount,

COUNT(*) AS transactionCount

FROM transactions

WHERE transactionType = 'EXPENSE'
AND dateMillis >= :startMillis AND dateMillis < :endMillis

GROUP BY date(dateMillis / 1000,'unixepoch','localtime')

ORDER BY dayMillis
"""
    )
    fun getDailyExpense(startMillis: Long, endMillis: Long): Flow<List<DailyExpenseEntity>>


    // The casts matter: read as text, "08" and "09" are taken for bad octal numbers and become 0
    @Query(
        """
SELECT

CAST(strftime('%Y', datetime(dateMillis / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS year,

CAST(strftime('%m', datetime(dateMillis / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS month,


SUM(
CASE 
WHEN transactionType = 'INCOME'
THEN amount
ELSE 0
END
) AS income,


SUM(
CASE 
WHEN transactionType = 'EXPENSE'
THEN amount
ELSE 0
END
) AS expense


FROM transactions

WHERE dateMillis >= :startMillis AND dateMillis < :endMillis

GROUP BY year, month

ORDER BY year, month

"""
    )
    fun getMonthlyTrend(startMillis: Long, endMillis: Long): Flow<List<MonthlyTrendEntity>>

}