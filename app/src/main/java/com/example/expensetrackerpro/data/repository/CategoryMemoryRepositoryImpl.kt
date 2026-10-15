package com.example.expensetrackerpro.data.repository

import com.example.expensetrackerpro.data.local.dao.CategoryMemoryDao
import com.example.expensetrackerpro.data.mapper.storageOutcome
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.data.mapper.toEntity
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.repository.CategoryMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryMemoryRepositoryImpl @Inject constructor(
    private val categoryMemoryDao: CategoryMemoryDao
) : CategoryMemoryRepository {

    override fun observeAll(): Flow<List<CategoryMemory>> =
        categoryMemoryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun remember(memory: CategoryMemory): Outcome<Unit> =
        storageOutcome { categoryMemoryDao.upsert(memory.toEntity()) }
}
