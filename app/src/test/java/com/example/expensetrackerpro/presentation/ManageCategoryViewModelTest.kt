package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.AddCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.DeleteCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoryUsageUseCase
import com.example.expensetrackerpro.domain.usecase.category.ReassignAndDeleteCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.SaveCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.SectionCategoryUsageUseCase
import com.example.expensetrackerpro.domain.usecase.category.UpdateCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorEvent
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorReducer
import com.example.expensetrackerpro.presentation.manageCategories.ManageCategoryViewModel
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryEvent
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryMessage
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class ManageCategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 10, hour = 15))

    private val food = category(id = 1, name = "Food", type = TransactionType.EXPENSE).copy(isCustom = false)
    private val transport = category(id = 2, name = "Transport", type = TransactionType.EXPENSE)
    private val gym = category(id = 3, name = "Gym", type = TransactionType.EXPENSE)
    private val salary = category(id = 4, name = "Salary", type = TransactionType.INCOME)

    // Food and Transport were used this month, Transport for more money; Gym never
    private val repository = FakeCategoryRepository(listOf(food, transport, gym, salary)).apply {
        setUsage(food.id, totalCount = 20, periodCount = 6, periodAmount = rupees(1200))
        setUsage(transport.id, totalCount = 8, periodCount = 2, periodAmount = rupees(3000))
    }

    private fun viewModel(): ManageCategoryViewModel {
        val validateCategoryName = ValidateCategoryNameUseCase()

        return ManageCategoryViewModel(
            getCategoryUsage = GetCategoryUsageUseCase(
                repository,
                GetCurrentPeriodUseCase(timeProvider),
                GetDateRangeUseCase(timeProvider)
            ),
            sectionCategoryUsage = SectionCategoryUsageUseCase(),
            saveCategory = SaveCategoryUseCase(
                GetCategoriesUseCase(repository),
                validateCategoryName,
                AddCategoryUseCase(repository),
                UpdateCategoryUseCase(repository)
            ),
            deleteCategory = DeleteCategoryUseCase(repository),
            reassignAndDeleteCategory = ReassignAndDeleteCategoryUseCase(repository),
            categoryEditorReducer = CategoryEditorReducer(validateCategoryName)
        )
    }

    private fun TestScope.subscribedViewModel() = viewModel().also { subscribe(it.uiState) }

    private fun ManageCategoryViewModel.editor(event: CategoryEditorEvent) =
        onEvent(ManageCategoryEvent.Editor(event))

    /** Opens [category] in the sheet and presses its delete button. */
    private fun ManageCategoryViewModel.askToDelete(category: Category) {
        onEvent(ManageCategoryEvent.CategoryClicked(category))
        editor(CategoryEditorEvent.DeleteClicked)
    }

    // --- List

    @Test
    fun showsExpenseCategoriesInTheirSections_withTheCountOfEachType() = runTest {
        val state = subscribedViewModel().uiState.value

        assertEquals(TransactionType.EXPENSE, state.selectedType)
        assertEquals(listOf(transport, food), state.sections.mostUsed.map { it.category })
        assertEquals(listOf(gym), state.sections.unused.map { it.category })
        assertEquals(3, state.sections.expenseCount)
        assertEquals(1, state.sections.incomeCount)
        assertNull(state.categoryEditor)
        assertNull(state.pendingDelete)
    }

    @Test
    fun rows_carryThisMonthsUsage() = runTest {
        val top = subscribedViewModel().uiState.value.sections.mostUsed.first()

        assertEquals(2, top.periodCount)
        assertEquals(rupees(3000), top.periodAmount)
        assertEquals(8, top.totalCount)
    }

    @Test
    fun typeChanged_showsIncomeCategories() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.onEvent(ManageCategoryEvent.TypeChanged(TransactionType.INCOME))
        val state = viewModel.uiState.value

        assertEquals(TransactionType.INCOME, state.selectedType)
        assertTrue(state.sections.mostUsed.isEmpty())
        assertEquals(listOf(salary), state.sections.unused.map { it.category })
    }

    @Test
    fun search_opensFiltersFromTheFirstLetter_andClosingClearsIt() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.onEvent(ManageCategoryEvent.SearchOpened)
        viewModel.onEvent(ManageCategoryEvent.SearchQueryChanged("g"))
        var state = viewModel.uiState.value

        assertTrue(state.isSearchOpen)
        assertTrue(state.sections.mostUsed.isEmpty())
        assertEquals(listOf(gym), state.sections.unused.map { it.category })
        // The tabs keep their totals
        assertEquals(3, state.sections.expenseCount)

        viewModel.onEvent(ManageCategoryEvent.SearchClosed)
        state = viewModel.uiState.value

        assertFalse(state.isSearchOpen)
        assertEquals("", state.searchQuery)
        assertEquals(2, state.sections.mostUsed.size)
    }

    @Test
    fun aChangeInUsage_movesTheCategoryBetweenSections() = runTest {
        val viewModel = subscribedViewModel()

        repository.setUsage(gym.id, totalCount = 1, periodCount = 1, periodAmount = rupees(5000))

        val sections = viewModel.uiState.value.sections
        assertEquals(listOf(gym, transport, food), sections.mostUsed.map { it.category })
        assertTrue(sections.unused.isEmpty())
    }

    // --- Add and edit

    @Test
    fun addClicked_opensAnEmptySheetOfTheSelectedType() = runTest {
        val viewModel = subscribedViewModel()
        viewModel.onEvent(ManageCategoryEvent.TypeChanged(TransactionType.INCOME))

        viewModel.onEvent(ManageCategoryEvent.AddClicked)
        val editor = requireNotNull(viewModel.uiState.value.categoryEditor)

        assertFalse(editor.isEditMode)
        assertEquals(TransactionType.INCOME, editor.type)
        assertEquals("", editor.name)
    }

    @Test
    fun creating_storesTheCategoryAndClosesTheSheet() = runTest {
        val viewModel = subscribedViewModel()
        viewModel.onEvent(ManageCategoryEvent.AddClicked)

        viewModel.editor(CategoryEditorEvent.NameChanged("Pets"))
        viewModel.editor(CategoryEditorEvent.IconSelected(CategoryIcon.PETS))
        viewModel.editor(CategoryEditorEvent.SaveClicked)

        val stored = repository.current.single { it.name == "Pets" }
        assertEquals(CategoryIcon.PETS, stored.icon)
        assertEquals(TransactionType.EXPENSE, stored.type)
        assertTrue(stored.isCustom)
        assertNull(viewModel.uiState.value.categoryEditor)
        assertTrue(viewModel.uiState.value.sections.unused.any { it.category == stored })
    }

    @Test
    fun aDuplicateName_showsTheErrorAndCannotBeSaved() = runTest {
        val viewModel = subscribedViewModel()
        viewModel.onEvent(ManageCategoryEvent.AddClicked)

        viewModel.editor(CategoryEditorEvent.NameChanged("food"))
        viewModel.editor(CategoryEditorEvent.SaveClicked)
        val editor = requireNotNull(viewModel.uiState.value.categoryEditor)

        assertEquals(CategoryNameValidation.DUPLICATE, editor.nameError)
        assertFalse(editor.isSaveEnabled)
        assertEquals(4, repository.current.size)
    }

    @Test
    fun aNameTakenWhileTheSheetIsOpen_isFlagged() = runTest {
        val viewModel = subscribedViewModel()
        viewModel.onEvent(ManageCategoryEvent.AddClicked)
        viewModel.editor(CategoryEditorEvent.NameChanged("Pets"))

        repository.insertCategory(category(id = 0, name = "pets"))

        assertEquals(
            CategoryNameValidation.DUPLICATE,
            viewModel.uiState.value.categoryEditor?.nameError
        )
    }

    @Test
    fun tappingARow_opensItForEditing_andSavingKeepsItsIdAndBuiltInFlag() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.onEvent(ManageCategoryEvent.CategoryClicked(food))
        val editor = requireNotNull(viewModel.uiState.value.categoryEditor)
        assertTrue(editor.isEditMode)
        assertEquals("Food", editor.name)

        viewModel.editor(CategoryEditorEvent.NameChanged("Eating out"))
        viewModel.editor(CategoryEditorEvent.IconSelected(CategoryIcon.RESTAURANT))
        viewModel.editor(CategoryEditorEvent.SaveClicked)

        assertEquals(
            food.copy(name = "Eating out", icon = CategoryIcon.RESTAURANT),
            repository.current.single { it.id == food.id }
        )
        assertEquals(4, repository.current.size)
        assertNull(viewModel.uiState.value.categoryEditor)
    }

    @Test
    fun dismissingTheSheet_savesNothing() = runTest {
        val viewModel = subscribedViewModel()
        viewModel.onEvent(ManageCategoryEvent.CategoryClicked(gym))
        viewModel.editor(CategoryEditorEvent.NameChanged("Fitness"))

        viewModel.editor(CategoryEditorEvent.Dismissed)

        assertNull(viewModel.uiState.value.categoryEditor)
        assertEquals(gym, repository.current.single { it.id == gym.id })
    }

    // --- Delete

    @Test
    fun deletingAnUnusedCategory_asksFirst_thenDeletesIt() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.askToDelete(gym)
        val request = requireNotNull(viewModel.uiState.value.pendingDelete)

        // The sheet made way for the dialog
        assertNull(viewModel.uiState.value.categoryEditor)
        assertEquals(gym, request.category)
        assertFalse(request.needsReassign)
        assertTrue(request.isConfirmEnabled)
        assertTrue(gym in repository.current)

        viewModel.onEvent(ManageCategoryEvent.DeleteConfirmed)

        assertFalse(gym in repository.current)
        assertNull(viewModel.uiState.value.pendingDelete)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun dismissingTheDeleteDialog_keepsTheCategory() = runTest {
        val viewModel = subscribedViewModel()
        viewModel.askToDelete(gym)

        viewModel.onEvent(ManageCategoryEvent.DeleteDismissed)
        // Nothing is waiting to be confirmed any more
        viewModel.onEvent(ManageCategoryEvent.DeleteConfirmed)

        assertNull(viewModel.uiState.value.pendingDelete)
        assertTrue(gym in repository.current)
    }

    @Test
    fun deletingACategoryWithTransactions_offersTheOthersOfItsType_andWaitsForAChoice() = runTest {
        val viewModel = subscribedViewModel()

        viewModel.askToDelete(food)
        val request = requireNotNull(viewModel.uiState.value.pendingDelete)

        assertTrue(request.needsReassign)
        assertEquals(20, request.transactionCount)
        // By name; neither itself nor the income category
        assertEquals(listOf(gym, transport), request.targets)
        assertFalse(request.isConfirmEnabled)

        // Confirming without a target does nothing
        viewModel.onEvent(ManageCategoryEvent.DeleteConfirmed)

        assertNotNull(viewModel.uiState.value.pendingDelete)
        assertTrue(food in repository.current)
    }

    @Test
    fun reassignThenDelete_movesTheTransactionsAndRemovesTheCategory() = runTest {
        val viewModel = subscribedViewModel()
        viewModel.askToDelete(food)

        viewModel.onEvent(ManageCategoryEvent.DeleteTargetSelected(gym.id))
        assertTrue(requireNotNull(viewModel.uiState.value.pendingDelete).isConfirmEnabled)

        viewModel.onEvent(ManageCategoryEvent.DeleteConfirmed)
        val state = viewModel.uiState.value

        assertFalse(food in repository.current)
        assertEquals(20, repository.totalCountOf(gym.id))
        assertNull(state.pendingDelete)
        assertNull(state.message)
        // Gym took over this month's use as well
        assertEquals(listOf(transport, gym), state.sections.mostUsed.map { it.category })
        assertEquals(2, state.sections.expenseCount)
    }

    @Test
    fun aTargetThatIsNotOffered_cannotBeConfirmed() = runTest {
        val viewModel = subscribedViewModel()
        viewModel.askToDelete(food)

        viewModel.onEvent(ManageCategoryEvent.DeleteTargetSelected(salary.id))
        viewModel.onEvent(ManageCategoryEvent.DeleteConfirmed)

        assertTrue(food in repository.current)
        assertEquals(0, repository.totalCountOf(salary.id))
    }

    @Test
    fun theOnlyCategoryOfItsType_cannotBeDeletedWhileItHasTransactions() = runTest {
        repository.setUsage(salary.id, totalCount = 3)
        val viewModel = subscribedViewModel()

        viewModel.askToDelete(salary)
        val request = requireNotNull(viewModel.uiState.value.pendingDelete)

        assertTrue(request.targets.isEmpty())
        assertFalse(request.isConfirmEnabled)

        viewModel.onEvent(ManageCategoryEvent.DeleteConfirmed)
        assertTrue(salary in repository.current)
    }

    @Test
    fun aReassignThatFails_keepsEverythingAndSaysSo() = runTest {
        repository.failReassign = true
        val viewModel = subscribedViewModel()
        viewModel.askToDelete(food)
        viewModel.onEvent(ManageCategoryEvent.DeleteTargetSelected(gym.id))

        viewModel.onEvent(ManageCategoryEvent.DeleteConfirmed)

        assertTrue(food in repository.current)
        assertEquals(20, repository.totalCountOf(food.id))
        assertEquals(ManageCategoryMessage.DELETE_FAILED, viewModel.uiState.value.message)

        viewModel.onEvent(ManageCategoryEvent.MessageShown)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun aCategoryThatGainsATransactionWhileTheDialogIsOpen_asksWhereToMoveItInstead() = runTest {
        val viewModel = subscribedViewModel()
        viewModel.askToDelete(gym)

        repository.setUsage(gym.id, totalCount = 1)
        viewModel.onEvent(ManageCategoryEvent.DeleteConfirmed)
        val request = requireNotNull(viewModel.uiState.value.pendingDelete)

        assertTrue(gym in repository.current)
        assertTrue(request.needsReassign)
        assertEquals(listOf(food, transport), request.targets)
    }
}
