package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.expensetrackerpro.data.local.model.CategoryExpenseEntity
import com.example.expensetrackerpro.data.local.model.DailyCashFlowEntity
import com.example.expensetrackerpro.data.local.model.DailyExpenseEntity
import com.example.expensetrackerpro.data.local.model.MonthlyTrendEntity
import kotlinx.coroutines.flow.Flow

/** The totals and breakdowns the dashboard reports are built from. Reads only. */
@Dao
interface DashboardDao {

    @Query("SELECT SUM(amountPaise) FROM transactions WHERE transactionType = 'INCOME' AND dateMillis >= :startMillis")
    fun getMonthlyIncome(startMillis: Long): Flow<Long?>

    @Query("SELECT SUM(amountPaise) FROM transactions WHERE transactionType = 'EXPENSE' AND dateMillis >= :startMillis")
    fun getMonthlyExpense(startMillis: Long): Flow<Long?>


    @Query(
        """
SELECT COALESCE(SUM(amountPaise),0)
FROM transactions
WHERE transactionType = 'INCOME'
AND dateMillis BETWEEN :startDate AND :endDate
"""
    )
    suspend fun getTotalIncome(
        startDate: Long,
        endDate: Long
    ): Long

    @Query(
        """
SELECT COALESCE(SUM(amountPaise),0)
FROM transactions
WHERE transactionType = 'EXPENSE'
AND dateMillis BETWEEN :startDate AND :endDate
"""
    )
    suspend fun getTotalExpense(
        startDate: Long,
        endDate: Long
    ): Long

    @Query(
        """
SELECT 
 c.id AS categoryId,
 c.name AS categoryName,
 c.icon AS icon,
 SUM(t.amountPaise) AS totalPaise

FROM transactions t
JOIN categories c
ON t.categoryId = c.id

WHERE t.transactionType = 'EXPENSE'
AND t.dateMillis >= :startMillis AND t.dateMillis < :endMillis

GROUP BY c.id

ORDER BY totalPaise DESC

"""
    )
    fun getCategoryExpense(startMillis: Long, endMillis: Long):
            Flow<List<CategoryExpenseEntity>>


    @Query("""
SELECT

dateMillis,

SUM(
CASE 
WHEN transactionType='INCOME' THEN amountPaise
ELSE 0
END
) incomePaise,


SUM(
CASE 
WHEN transactionType='EXPENSE' THEN amountPaise
ELSE 0
END
) expensePaise


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

SUM(amountPaise) AS amountPaise,

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
THEN amountPaise
ELSE 0
END
) AS incomePaise,


SUM(
CASE 
WHEN transactionType = 'EXPENSE'
THEN amountPaise
ELSE 0
END
) AS expensePaise


FROM transactions

WHERE dateMillis >= :startMillis AND dateMillis < :endMillis

GROUP BY year, month

ORDER BY year, month

"""
    )
    fun getMonthlyTrend(startMillis: Long, endMillis: Long): Flow<List<MonthlyTrendEntity>>

}