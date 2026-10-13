package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.model.CategoryUsageEntity
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    abstract suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories")
    abstract fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type")
    abstract fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    // Every category comes back, the unused ones with zeros
    @Query(
        """
SELECT c.*,
COUNT(t.id) AS totalCount,
COALESCE(SUM(CASE WHEN t.dateMillis >= :startMillis AND t.dateMillis < :endMillis THEN 1 ELSE 0 END), 0) AS periodCount,
COALESCE(SUM(CASE WHEN t.dateMillis >= :startMillis AND t.dateMillis < :endMillis THEN t.amountPaise ELSE 0 END), 0) AS periodPaise
FROM categories c
LEFT JOIN transactions t ON t.categoryId = c.id
GROUP BY c.id
"""
    )
    abstract fun getCategoryUsage(startMillis: Long, endMillis: Long): Flow<List<CategoryUsageEntity>>

    @Update
    abstract suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    abstract suspend fun getCategoryById(id: Int): CategoryEntity?

    @Query("UPDATE transactions SET categoryId = :toId WHERE categoryId = :fromId")
    abstract suspend fun moveTransactions(fromId: Int, toId: Int)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun deleteCategoryById(id: Int)

    // A built-in the user has renamed since it was read is left alone
    @Query("UPDATE categories SET name = :name WHERE id = :id AND builtInKey IS NOT NULL")
    abstract suspend fun renameBuiltIn(id: Int, name: String)

    /** Gives each built-in in [namesById] its new name. All or nothing. */
    @Transaction
    open suspend fun renameBuiltIns(namesById: Map<Int, String>) {
        namesById.forEach { (id, name) -> renameBuiltIn(id, name) }
    }

    /**
     * Files every transaction of [fromId] under [toId], then deletes [fromId]. All or nothing.
     * Returns false, changing nothing, unless both exist, differ and share a type.
     */
    @Transaction
    open suspend fun reassignAndDelete(fromId: Int, toId: Int): Boolean {
        val from = getCategoryById(fromId) ?: return false
        val to = getCategoryById(toId) ?: return false
        if (from.id == to.id || from.type != to.type) return false

        moveTransactions(fromId = from.id, toId = to.id)
        deleteCategoryById(from.id)

        return true
    }
}
