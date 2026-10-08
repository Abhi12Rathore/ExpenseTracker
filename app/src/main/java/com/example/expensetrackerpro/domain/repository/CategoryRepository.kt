package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getAllCategories(): Flow<List<Category>>

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    suspend fun insertCategory(category: Category)

    suspend fun updateCategory(category: Category)

    /** Returns false, leaving the category in place, when transactions still use it. */
    suspend fun deleteCategory(category: Category): Boolean
}
