package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.presentation.main.MainViewModel
import com.example.expensetrackerpro.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun notReadyUntilCategoriesFirstLoad() = runTest {
        val repository = PendingCategoryRepository()
        val viewModel = MainViewModel(GetCategoriesUseCase(repository))

        assertFalse(viewModel.uiState.value.isReady)

        repository.emit(listOf(category(id = 1, name = "Food", type = TransactionType.EXPENSE)))

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun readyOnceCategoriesAreLoaded() = runTest {
        val repository = FakeCategoryRepository(
            listOf(category(id = 1, name = "Food", type = TransactionType.EXPENSE))
        )

        val viewModel = MainViewModel(GetCategoriesUseCase(repository))

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun readyEvenWhenThereAreNoCategories() = runTest {
        // Every category can be deleted, so an empty first load must not hold the splash
        val viewModel = MainViewModel(GetCategoriesUseCase(FakeCategoryRepository()))

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun readyEvenWhenTheFirstLoadNeverDeliversCategories() = runTest {
        // The load ends abnormally before any list arrives; the splash must still be released
        val repository = object : CategoryRepository by FakeCategoryRepository() {
            override fun getAllCategories(): Flow<List<Category>> =
                flow { throw CancellationException("load aborted") }
        }

        val viewModel = MainViewModel(GetCategoriesUseCase(repository))

        assertTrue(viewModel.uiState.value.isReady)
    }
}

/** Emits nothing until [emit] is called, like a database that is still opening. */
private class PendingCategoryRepository : CategoryRepository {

    private val categories = MutableSharedFlow<List<Category>>()

    suspend fun emit(list: List<Category>) = categories.emit(list)

    override fun getAllCategories(): Flow<List<Category>> = categories

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categories.map { list -> list.filter { it.type == type } }

    override suspend fun insertCategory(category: Category) = Unit

    override suspend fun updateCategory(category: Category) = Unit

    override suspend fun deleteCategory(category: Category): Boolean = true
}
