package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getAllCategories(): Flow<List<Category>>

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    /** Returns the id the category was stored under. */
    suspend fun insertCategory(category: Category): Outcome<CategoryId>

    suspend fun updateCategory(category: Category): Outcome<Unit>

    /**
     * Gives each built-in category in [namesById] the name the app has for it. All or nothing;
     * one the user has renamed in the meantime keeps their name.
     */
    suspend fun renameBuiltInCategories(namesById: Map<CategoryId, String>): Outcome<Unit>

    /** Fails with CategoryInUse, leaving the category in place, when transactions still use it. */
    suspend fun deleteCategory(category: Category): Outcome<Unit>

    /** Every category with its transaction count ever, and its count and amount within [range]. */
    fun getCategoryUsage(range: DateRange): Flow<List<CategoryUsage>>

    /**
     * Moves the transactions of [category] to [target] and deletes [category], all or nothing.
     * Fails with NotFound, changing nothing, unless both are stored, differ and share a type.
     */
    suspend fun deleteCategoryReassigning(category: Category, target: Category): Outcome<Unit>
}
