package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import com.example.expensetrackerpro.domain.repository.ThemePreferenceRepository
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeThemePreferenceRepository
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.presentation.main.MainViewModel
import com.example.expensetrackerpro.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun mainViewModel(
        categoryRepository: CategoryRepository = FakeCategoryRepository(),
        themeRepository: ThemePreferenceRepository = FakeThemePreferenceRepository()
    ) = MainViewModel(
        GetCategoriesUseCase(categoryRepository),
        GetThemeModeUseCase(themeRepository)
    )

    @Test
    fun exposesTheSavedThemeMode() = runTest {
        val viewModel = mainViewModel(themeRepository = FakeThemePreferenceRepository(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun themeModeFollowsLaterChanges() = runTest {
        val themeRepository = FakeThemePreferenceRepository(ThemeMode.LIGHT)
        val viewModel = mainViewModel(themeRepository = themeRepository)

        themeRepository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun notReadyUntilTheThemeModeIsLoaded() = runTest {
        // Categories are already there, so only the theme is holding the splash
        val themeRepository = PendingThemePreferenceRepository()
        val viewModel = mainViewModel(themeRepository = themeRepository)

        assertFalse(viewModel.uiState.value.isReady)

        themeRepository.emit(ThemeMode.DARK)

        assertTrue(viewModel.uiState.value.isReady)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun notReadyUntilCategoriesFirstLoad() = runTest {
        val repository = PendingCategoryRepository()
        val viewModel = mainViewModel(repository)

        assertFalse(viewModel.uiState.value.isReady)

        repository.emit(listOf(category(id = 1, name = "Food", type = TransactionType.EXPENSE)))

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun readyOnceCategoriesAreLoaded() = runTest {
        val repository = FakeCategoryRepository(
            listOf(category(id = 1, name = "Food", type = TransactionType.EXPENSE))
        )

        val viewModel = mainViewModel(repository)

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun readyEvenWhenThereAreNoCategories() = runTest {
        // Every category can be deleted, so an empty first load must not hold the splash
        val viewModel = mainViewModel(FakeCategoryRepository())

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun readyEvenWhenTheFirstLoadNeverDeliversCategories() = runTest {
        // The load ends abnormally before any list arrives; the splash must still be released
        val repository = object : CategoryRepository by FakeCategoryRepository() {
            override fun getAllCategories(): Flow<List<Category>> =
                flow { throw CancellationException("load aborted") }
        }

        val viewModel = mainViewModel(repository)

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

/** Emits nothing until [emit] is called, like preferences that are still being read from disk. */
private class PendingThemePreferenceRepository : ThemePreferenceRepository {

    private val themeMode = MutableSharedFlow<ThemeMode>()

    suspend fun emit(mode: ThemeMode) = themeMode.emit(mode)

    override fun getThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = Unit
}
