package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.AddCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.DeleteCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.FilterCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.category.UpdateCategoryUseCase
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.presentation.manageCategories.ManageCategoryViewModel
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryEvent
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ManageCategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val food = category(id = 1, name = "Food", type = TransactionType.EXPENSE)
    private val transport = category(id = 2, name = "Transport", type = TransactionType.EXPENSE)
    private val salary = category(id = 3, name = "Salary", type = TransactionType.INCOME)

    // Transport still has transactions, so it can't be deleted
    private val repository =
        FakeCategoryRepository(listOf(food, transport, salary), idsInUse = setOf(transport.id))

    private fun viewModel() = ManageCategoryViewModel(
        getCategories = GetCategoriesUseCase(repository),
        filterCategories = FilterCategoriesUseCase(),
        addCategory = AddCategoryUseCase(repository),
        updateCategory = UpdateCategoryUseCase(repository),
        deleteCategory = DeleteCategoryUseCase(repository)
    )

    @Test
    fun showsExpenseCategoriesByDefault() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertEquals(TransactionType.EXPENSE, state.selectedType)
        assertEquals(listOf(food, transport), state.categories)
        assertFalse(state.isSheetOpen)
    }

    @Test
    fun typeChanged_showsIncomeCategories() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ManageCategoryEvent.TypeChanged(TransactionType.INCOME))

        assertEquals(listOf(salary), viewModel.uiState.value.categories)
    }

    @Test
    fun search_onlyFiltersFromThreeCharacters() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ManageCategoryEvent.SearchQueryChanged("tr"))
        assertEquals("tr", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(food, transport), viewModel.uiState.value.categories)

        viewModel.onEvent(ManageCategoryEvent.SearchQueryChanged("TRA"))
        assertEquals(listOf(transport), viewModel.uiState.value.categories)

        viewModel.onEvent(ManageCategoryEvent.SearchQueryChanged(""))
        assertEquals(listOf(food, transport), viewModel.uiState.value.categories)
    }

    @Test
    fun addClicked_thenSaved_addsCustomCategoryAndClosesSheet() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ManageCategoryEvent.AddClicked)
        assertTrue(viewModel.uiState.value.isSheetOpen)
        assertNull(viewModel.uiState.value.categoryToEdit)

        viewModel.onEvent(ManageCategoryEvent.CategorySaved("Gym", CategoryIcon.HOME, TransactionType.EXPENSE))
        val state = viewModel.uiState.value

        assertFalse(state.isSheetOpen)
        assertEquals(listOf("Food", "Transport", "Gym"), state.categories.map { it.name })
        assertTrue(state.categories.last().isCustom)
        assertEquals(4, repository.current.size)
    }

    @Test
    fun editClicked_thenSaved_updatesSameIdInsteadOfAdding() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ManageCategoryEvent.EditClicked(food))
        assertTrue(viewModel.uiState.value.isSheetOpen)
        assertEquals(food, viewModel.uiState.value.categoryToEdit)

        viewModel.onEvent(ManageCategoryEvent.CategorySaved("Groceries", CategoryIcon.SHOPPING_BAG, TransactionType.EXPENSE))
        val state = viewModel.uiState.value

        assertFalse(state.isSheetOpen)
        assertNull(state.categoryToEdit)
        assertEquals(3, repository.current.size)
        val edited = repository.current.first { it.id == food.id }
        assertEquals("Groceries", edited.name)
        assertEquals(CategoryIcon.SHOPPING_BAG, edited.icon)
    }

    @Test
    fun sheetDismissed_clearsEditTarget() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ManageCategoryEvent.EditClicked(food))
        viewModel.onEvent(ManageCategoryEvent.SheetDismissed)
        val state = viewModel.uiState.value

        assertFalse(state.isSheetOpen)
        assertNull(state.categoryToEdit)
        assertEquals(3, repository.current.size)
    }

    @Test
    fun addAfterDismissedEdit_addsRatherThanUpdates() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ManageCategoryEvent.EditClicked(food))
        viewModel.onEvent(ManageCategoryEvent.SheetDismissed)
        viewModel.onEvent(ManageCategoryEvent.AddClicked)
        viewModel.onEvent(ManageCategoryEvent.CategorySaved("Gym", CategoryIcon.HOME, TransactionType.EXPENSE))

        assertEquals(4, repository.current.size)
        assertEquals("Food", repository.current.first { it.id == food.id }.name)
    }

    @Test
    fun deleteClicked_removesCategory() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ManageCategoryEvent.DeleteClicked(food))

        assertEquals(listOf(transport), viewModel.uiState.value.categories)
        assertNull(viewModel.uiState.value.deleteBlockedCategoryName)
    }

    @Test
    fun deleteClicked_categoryWithTransactions_keepsItAndShowsMessageUntilAcknowledged() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ManageCategoryEvent.DeleteClicked(transport))

        assertEquals(listOf(food, transport), viewModel.uiState.value.categories)
        assertEquals("Transport", viewModel.uiState.value.deleteBlockedCategoryName)

        viewModel.onEvent(ManageCategoryEvent.DeleteBlockedMessageShown)

        assertNull(viewModel.uiState.value.deleteBlockedCategoryName)
    }
}
