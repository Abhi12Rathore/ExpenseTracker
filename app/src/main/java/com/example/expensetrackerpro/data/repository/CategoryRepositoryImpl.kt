package com.example.expensetrackerpro.data.repository

import com.example.expensetrackerpro.data.local.dao.CategoryDao
import com.example.expensetrackerpro.data.mapper.storageOutcome
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.data.mapper.toEntity
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type).map { list -> list.map { it.toDomain() } }

    override suspend fun insertCategory(category: Category): Outcome<CategoryId> =
        storageOutcome { CategoryId(categoryDao.insertCategory(category.toEntity()).toInt()) }

    override suspend fun updateCategory(category: Category): Outcome<Unit> =
        storageOutcome { categoryDao.update(category.toEntity()) }

    // Transactions that reference the category stop the delete (foreign key RESTRICT)
    override suspend fun deleteCategory(category: Category): Outcome<Unit> =
        storageOutcome { categoryDao.deleteCategory(category.toEntity()) }

    override fun getCategoryUsage(range: DateRange): Flow<List<CategoryUsage>> =
        categoryDao.getCategoryUsage(range.startMillis, range.endMillis)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun deleteCategoryReassigning(category: Category, target: Category): Outcome<Unit> =
        when (
            val moved = storageOutcome {
                categoryDao.reassignAndDelete(fromId = category.id.value, toId = target.id.value)
            }
        ) {
            is Outcome.Success ->
                if (moved.value) Outcome.Success(Unit) else Outcome.Failure(DomainError.NotFound)
            is Outcome.Failure -> moved
        }
}
