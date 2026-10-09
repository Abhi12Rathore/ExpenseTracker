package com.example.expensetrackerpro.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.data.mapper.toEntity
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        transactionDao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        transactionDao.getCategoriesByType(type).map { list -> list.map { it.toDomain() } }

    override suspend fun insertCategory(category: Category): Int =
        transactionDao.insertCategory(category.toEntity()).toInt()

    override suspend fun updateCategory(category: Category) {
        transactionDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category): Boolean =
        try {
            transactionDao.deleteCategory(category.toEntity())
            true
        } catch (e: SQLiteConstraintException) {
            // Transactions reference this category (foreign key RESTRICT)
            false
        }
}
