package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.category
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryWriteUseCasesTest {

    private val food = category(id = 1, name = "Food")
    private val salary = category(id = 2, name = "Salary", type = TransactionType.INCOME)

    @Test
    fun addCategory_storesIt() = runTest {
        val repository = FakeCategoryRepository()

        val id = AddCategoryUseCase(repository)(food)

        assertEquals(Outcome.Success(food.id), id)

        assertEquals(listOf(food), repository.current)
    }

    @Test
    fun updateCategory_replacesOnlyTheMatchingId() = runTest {
        val repository = FakeCategoryRepository(listOf(food, salary))
        val renamed = food.copy(name = "Groceries", icon = CategoryIcon.SHOPPING_BAG)

        UpdateCategoryUseCase(repository)(renamed)

        assertEquals(listOf(renamed, salary), repository.current)
    }

    @Test
    fun addAndUpdate_reportADatabaseThatCannotWrite_andStoreNothing() = runTest {
        val storageFailure = DomainError.StorageFailure("disk I/O error")
        val repository = FakeCategoryRepository(listOf(food)).apply { saveFailure = storageFailure }

        assertEquals(Outcome.Failure(storageFailure), AddCategoryUseCase(repository)(salary))
        assertEquals(
            Outcome.Failure(storageFailure),
            UpdateCategoryUseCase(repository)(food.copy(name = "Groceries"))
        )

        assertEquals(listOf(food), repository.current)
    }

    @Test
    fun deleteCategory_removesOnlyTheMatchingId() = runTest {
        val repository = FakeCategoryRepository(listOf(food, salary))

        val deleted = DeleteCategoryUseCase(repository)(food)

        assertEquals(Outcome.Success(Unit), deleted)
        assertEquals(listOf(salary), repository.current)
    }

    @Test
    fun deleteCategory_withTransactions_keepsItAndReportsFailure() = runTest {
        val repository = FakeCategoryRepository(listOf(food, salary), idsInUse = setOf(food.id))

        val deleted = DeleteCategoryUseCase(repository)(food)

        assertEquals(Outcome.Failure(DomainError.CategoryInUse), deleted)
        assertEquals(listOf(food, salary), repository.current)
    }
}
