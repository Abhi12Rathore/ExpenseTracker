package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getAllCategories(): Flow<List<Category>>

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    /** Returns the id the category was stored under. */
    suspend fun insertCategory(category: Category): Int

    suspend fun updateCategory(category: Category)

    /** Returns false, leaving the category in place, when transactions still use it. */
    suspend fun deleteCategory(category: Category): Boolean

    /** Every category with its transaction count ever, and its count and amount within [range]. */
    fun getCategoryUsage(range: DateRange): Flow<List<CategoryUsage>>

    /**
     * Moves the transactions of [category] to [target] and deletes [category], all or nothing.
     * Returns false when nothing was changed.
     */
    suspend fun deleteCategoryReassigning(category: Category, target: Category): Boolean
}
