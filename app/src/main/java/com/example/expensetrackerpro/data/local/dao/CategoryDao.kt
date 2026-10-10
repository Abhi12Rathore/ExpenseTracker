package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.model.CategoryUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CategoryDao {

    // Every category comes back, the unused ones with zeros
    @Query(
        """
SELECT c.*,
COUNT(t.id) AS totalCount,
COALESCE(SUM(CASE WHEN t.dateMillis >= :startMillis AND t.dateMillis < :endMillis THEN 1 ELSE 0 END), 0) AS periodCount,
COALESCE(SUM(CASE WHEN t.dateMillis >= :startMillis AND t.dateMillis < :endMillis THEN t.amount ELSE 0 END), 0) AS periodAmount
FROM categories c
LEFT JOIN transactions t ON t.categoryId = c.id
GROUP BY c.id
"""
    )
    abstract fun getCategoryUsage(startMillis: Long, endMillis: Long): Flow<List<CategoryUsageEntity>>

    @Update
    abstract suspend fun updateCategoryRow(category: CategoryEntity)

    @Query("UPDATE transactions SET categoryName = :name WHERE categoryId = :id")
    abstract suspend fun renameOnTransactions(id: Int, name: String)

    /** Stores [category], and its name on every transaction that keeps a copy of it. All or nothing. */
    @Transaction
    open suspend fun update(category: CategoryEntity) {
        updateCategoryRow(category)
        renameOnTransactions(id = category.id, name = category.name)
    }

    @Query("SELECT * FROM categories WHERE id = :id")
    abstract suspend fun getCategoryById(id: Int): CategoryEntity?

    // The transaction's copy of the category name moves with it
    @Query("UPDATE transactions SET categoryId = :toId, categoryName = :toName WHERE categoryId = :fromId")
    abstract suspend fun moveTransactions(fromId: Int, toId: Int, toName: String)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun deleteCategoryById(id: Int)

    /**
     * Files every transaction of [fromId] under [toId], then deletes [fromId]. All or nothing.
     * Returns false, changing nothing, unless both exist, differ and share a type.
     */
    @Transaction
    open suspend fun reassignAndDelete(fromId: Int, toId: Int): Boolean {
        val from = getCategoryById(fromId) ?: return false
        val to = getCategoryById(toId) ?: return false
        if (from.id == to.id || from.type != to.type) return false

        moveTransactions(fromId = from.id, toId = to.id, toName = to.name)
        deleteCategoryById(from.id)

        return true
    }
}
