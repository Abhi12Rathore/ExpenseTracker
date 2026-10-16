package com.example.expensetrackerpro.data.repository

import com.example.expensetrackerpro.data.local.dao.CategoryMemoryDao
import com.example.expensetrackerpro.data.local.entity.CategoryMemoryEntity
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryMemoryRepositoryImplTest {

    private class InMemoryDao : CategoryMemoryDao {
        val rows = MutableStateFlow(emptyList<CategoryMemoryEntity>())

        override fun observeAll(): Flow<List<CategoryMemoryEntity>> = rows

        override suspend fun upsert(memory: CategoryMemoryEntity) {
            rows.value = rows.value + memory
        }
    }

    private val dao = InMemoryDao()
    private val repository = CategoryMemoryRepositoryImpl(dao)

    @Test
    fun remember_storesTheTitleKeyTypeAndCategory_andReadsThemBack() = runTest {
        val memory = CategoryMemory("dmart", TransactionType.EXPENSE, CategoryId(4))

        assertEquals(Outcome.Success(Unit), repository.remember(memory))

        assertEquals(listOf(CategoryMemoryEntity("dmart", TransactionType.EXPENSE, 4)), dao.rows.value)
        assertEquals(listOf(memory), repository.observeAll().first())
    }
}
