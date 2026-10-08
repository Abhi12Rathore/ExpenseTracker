package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryReadUseCasesTest {

    private val food = category(id = 1, name = "Food", type = TransactionType.EXPENSE)
    private val salary = category(id = 2, name = "Salary", type = TransactionType.INCOME)
    private val bills = category(id = 3, name = "Bills", type = TransactionType.EXPENSE)
    private val repository = FakeCategoryRepository(listOf(food, salary, bills))

    @Test
    fun getCategories_returnsAllTypes() = runTest {
        assertEquals(listOf(food, salary, bills), GetCategoriesUseCase(repository)().first())
    }

    @Test
    fun getCategories_emptyRepository_isEmpty() = runTest {
        assertTrue(GetCategoriesUseCase(FakeCategoryRepository())().first().isEmpty())
    }

    @Test
    fun getCategoriesByType_returnsOnlyThatType() = runTest {
        val getByType = GetCategoriesByTypeUseCase(repository)

        assertEquals(listOf(food, bills), getByType(TransactionType.EXPENSE).first())
        assertEquals(listOf(salary), getByType(TransactionType.INCOME).first())
    }
}
