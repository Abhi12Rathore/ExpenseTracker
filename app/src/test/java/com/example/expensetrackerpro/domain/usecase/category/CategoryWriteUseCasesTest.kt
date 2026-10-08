package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.category
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryWriteUseCasesTest {

    private val food = category(id = 1, name = "Food")
    private val salary = category(id = 2, name = "Salary", type = TransactionType.INCOME)

    @Test
    fun addCategory_storesIt() = runTest {
        val repository = FakeCategoryRepository()

        AddCategoryUseCase(repository)(food)

        assertEquals(listOf(food), repository.current)
    }

    @Test
    fun updateCategory_replacesOnlyTheMatchingId() = runTest {
        val repository = FakeCategoryRepository(listOf(food, salary))
        val renamed = food.copy(name = "Groceries", iconName = "Shopping")

        UpdateCategoryUseCase(repository)(renamed)

        assertEquals(listOf(renamed, salary), repository.current)
    }

    @Test
    fun deleteCategory_removesOnlyTheMatchingId() = runTest {
        val repository = FakeCategoryRepository(listOf(food, salary))

        val deleted = DeleteCategoryUseCase(repository)(food)

        assertTrue(deleted)
        assertEquals(listOf(salary), repository.current)
    }

    @Test
    fun deleteCategory_withTransactions_keepsItAndReportsFailure() = runTest {
        val repository = FakeCategoryRepository(listOf(food, salary), idsInUse = setOf(food.id))

        val deleted = DeleteCategoryUseCase(repository)(food)

        assertFalse(deleted)
        assertEquals(listOf(food, salary), repository.current)
    }
}
