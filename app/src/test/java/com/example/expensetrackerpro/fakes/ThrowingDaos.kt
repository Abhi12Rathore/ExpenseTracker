package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.data.local.dao.AppDataDao
import com.example.expensetrackerpro.data.local.dao.CategoryDao
import com.example.expensetrackerpro.data.local.dao.SampleDataDao
import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.model.CategoryUsageEntity
import com.example.expensetrackerpro.data.local.model.TransactionWithCategory
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// DAOs whose writes all throw [error], for failures a real database can't be made to produce

class ThrowingTransactionDao(private val error: Exception) : TransactionDao {

    override suspend fun insertTransaction(transaction: TransactionEntity) = throw error

    override suspend fun updateTransaction(transaction: TransactionEntity) = throw error

    override suspend fun deleteTransaction(transaction: TransactionEntity) = throw error

    override suspend fun getTransactionById(id: Int): TransactionWithCategory? = null

    override suspend fun getLatestTransaction(): TransactionWithCategory? = null

    override suspend fun countTransactions(): Int = 0

    override fun getAllTransactions(): Flow<List<TransactionWithCategory>> = emptyFlow()

    override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionWithCategory>> = emptyFlow()

    override fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionWithCategory>> =
        emptyFlow()

    override fun searchTransactionsBetween(
        startMillis: Long,
        endMillis: Long,
        query: String
    ): Flow<List<TransactionWithCategory>> = emptyFlow()
}

class ThrowingCategoryDao(private val error: Exception) : CategoryDao() {

    override suspend fun insertCategory(category: CategoryEntity): Long = throw error

    override suspend fun deleteCategory(category: CategoryEntity) = throw error

    override suspend fun renameBuiltIn(id: Int, name: String) = throw error

    override fun getAllCategories(): Flow<List<CategoryEntity>> = emptyFlow()

    override fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> = emptyFlow()

    override fun getCategoryUsage(startMillis: Long, endMillis: Long): Flow<List<CategoryUsageEntity>> = emptyFlow()

    override suspend fun update(category: CategoryEntity) = throw error

    // Both are found, so the reassigning delete gets as far as its first write
    override suspend fun getCategoryById(id: Int): CategoryEntity =
        CategoryEntity(id = id, name = "Category $id", type = TransactionType.EXPENSE, icon = category().icon)

    override suspend fun moveTransactions(fromId: Int, toId: Int) = throw error

    override suspend fun deleteCategoryById(id: Int) = throw error
}

class ThrowingAppDataDao(private val error: Exception) : AppDataDao() {

    override suspend fun getAllCategories(): List<CategoryEntity> = emptyList()

    override suspend fun getAllTransactions(): List<TransactionEntity> = emptyList()

    override suspend fun insertCategories(categories: List<CategoryEntity>) = throw error

    override suspend fun insertTransactions(transactions: List<TransactionEntity>) = throw error

    override suspend fun deleteAllTransactions() = throw error

    override suspend fun deleteCustomCategories() = throw error

    override suspend fun deleteAllCategories() = throw error
}

class ThrowingSampleDataDao(private val error: Exception) : SampleDataDao() {

    override suspend fun getCategories(): List<CategoryEntity> = emptyList()

    override suspend fun insertCategories(categories: List<CategoryEntity>) = throw error

    override suspend fun insertTransactions(transactions: List<TransactionEntity>) = throw error

    override suspend fun deleteAllTransactions() = throw error
}
