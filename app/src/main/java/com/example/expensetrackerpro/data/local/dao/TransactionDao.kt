package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetrackerpro.data.local.entity.Category
import com.example.expensetrackerpro.data.local.entity.Transaction
import com.example.expensetrackerpro.data.local.entity.TransactionType
import com.example.expensetrackerpro.data.local.model.DailyCashFlowEntity
import kotlinx.coroutines.flow.Flow
import com.example.expensetrackerpro.data.local.model.CategoryExpenseEntity
import com.example.expensetrackerpro.data.local.model.MonthlyTrendEntity

@Dao
interface TransactionDao {

    // --- Transaction CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    // --- Category CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // --- Existing Queries (Flows) ---
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE transactionType = :type ORDER BY dateMillis DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE dateMillis >= :startTimestamp ORDER BY dateMillis DESC")
    fun getFilteredTransactions(startTimestamp: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type")
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    // --- Financial Stats ---
    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'INCOME' AND dateMillis >= :startMillis")
    fun getMonthlyIncome(startMillis: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'EXPENSE' AND dateMillis >= :startMillis")
    fun getMonthlyExpense(startMillis: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<Category>)


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
 SUM(t.amount) AS totalAmount

FROM transactions t
JOIN categories c
ON t.categoryId = c.id

WHERE t.transactionType = 'EXPENSE'

GROUP BY c.id

ORDER BY totalAmount DESC

"""
    )
    fun getCategoryExpense():
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

GROUP BY date(dateMillis / 1000,'unixepoch')

ORDER BY dateMillis
""")
    fun getDailyCashFlow(): Flow<List<DailyCashFlowEntity>>


    @Query(
        """
SELECT

strftime('%Y', datetime(dateMillis / 1000, 'unixepoch')) AS year,

strftime('%m', datetime(dateMillis / 1000, 'unixepoch')) AS month,


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


GROUP BY year, month

ORDER BY year, month

"""
    )
    fun getMonthlyTrend(): Flow<List<MonthlyTrendEntity>>

}