package com.example.expensetrackerpro.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.AddCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesByTypeUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.AddTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionByIdUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateTransactionInputUseCase
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.AddEditTransactionViewModel
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEvent
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddEditTransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val food = category(id = 1, name = "Food", type = TransactionType.EXPENSE)
    private val bills = category(id = 2, name = "Bills", type = TransactionType.EXPENSE)
    private val salary = category(id = 3, name = "Salary", type = TransactionType.INCOME)

    private val categories = FakeCategoryRepository(listOf(food, bills, salary))
    private val transactions = FakeTransactionRepository()

    private fun viewModel(transactionId: Int? = null) = AddEditTransactionViewModel(
        getTransactionById = GetTransactionByIdUseCase(transactions),
        getCategories = GetCategoriesUseCase(categories),
        getCategoriesByType = GetCategoriesByTypeUseCase(categories),
        addTransaction = AddTransactionUseCase(transactions),
        updateTransaction = UpdateTransactionUseCase(transactions),
        validateTransactionInput = ValidateTransactionInputUseCase(),
        addCategory = AddCategoryUseCase(categories),
        savedStateHandle = SavedStateHandle(
            if (transactionId != null) mapOf("transactionId" to transactionId) else emptyMap()
        )
    )

    @Test
    fun addMode_startsEmptyWithExpenseCategories() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertFalse(state.isEditMode)
        assertFalse(state.isSaveEnabled)
        assertEquals(TransactionType.EXPENSE, state.transactionType)
        assertEquals(listOf(food, bills), state.availableCategories)
    }

    @Test
    fun missingTransactionIdArgument_orMinusOne_isAddMode() = runTest {
        val viewModel = viewModel(transactionId = -1)
        subscribe(viewModel.uiState)

        assertFalse(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun saveEnabledOnlyWithAmountNoteAndCategory() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("120"))
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("Lunch"))
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(food))
        assertTrue(viewModel.uiState.value.isSaveEnabled)

        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("  "))
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun nonNumericAmount_isIgnored() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("12"))
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("12a"))

        assertEquals("12", viewModel.uiState.value.amount)
    }

    @Test
    fun typeChange_clearsCategoryAndSwapsAvailableCategories() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("120"))
        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("Lunch"))
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(food))

        viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.INCOME))
        val state = viewModel.uiState.value

        assertNull(state.selectedCategory)
        assertFalse(state.isSaveEnabled)
        assertEquals(listOf(salary), state.availableCategories)
    }

    @Test
    fun dateSelected_updatesDate_andNullIsIgnored() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(AddEditTransactionEvent.DateSelected(1234L))
        viewModel.onEvent(AddEditTransactionEvent.DateSelected(null))

        assertEquals(1234L, viewModel.uiState.value.dateMillis)
    }

    @Test
    fun categorySheet_opensDismissesAndClosesAfterAdding() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(AddEditTransactionEvent.CategorySheetOpened)
        assertTrue(viewModel.uiState.value.isCategorySheetVisible)
        viewModel.onEvent(AddEditTransactionEvent.CategorySheetDismissed)
        assertFalse(viewModel.uiState.value.isCategorySheetVisible)

        viewModel.onEvent(AddEditTransactionEvent.CategorySheetOpened)
        viewModel.onEvent(AddEditTransactionEvent.CategoryAdded("Gym", "Home", TransactionType.EXPENSE))
        val state = viewModel.uiState.value

        assertFalse(state.isCategorySheetVisible)
        assertEquals(listOf("Food", "Bills", "Gym"), state.availableCategories.map { it.name })
        assertTrue(state.availableCategories.last().isCustom)
    }

    @Test
    fun saveInAddMode_storesTransactionAndResetsForm() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("120.5"))
        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("Lunch"))
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(food))
        viewModel.onEvent(AddEditTransactionEvent.DateSelected(999L))

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        val saved = transactions.current.single()
        assertEquals(0, saved.id)
        assertEquals(120.5, saved.amount, 0.001)
        assertEquals("Lunch", saved.note)
        assertEquals("Lunch", saved.title)
        assertEquals(food.id, saved.categoryId)
        assertEquals("Food", saved.categoryName)
        assertEquals(TransactionType.EXPENSE, saved.transactionType)
        assertEquals(999L, saved.dateMillis)

        val state = viewModel.uiState.value
        assertEquals("", state.amount)
        assertEquals("", state.note)
        assertNull(state.selectedCategory)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun saveWhileInvalid_storesNothing() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("120"))

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertTrue(transactions.current.isEmpty())
    }

    @Test
    fun editMode_prefillsFromStoredTransaction() = runTest {
        transactions.saveTransaction(
            transaction(id = 5, amount = 5000.0, type = TransactionType.INCOME, dateMillis = 42L, note = "July pay")
                .copy(categoryId = salary.id, categoryName = "Salary")
        )

        val viewModel = viewModel(transactionId = 5)
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertTrue(state.isEditMode)
        assertEquals("5000.0", state.amount)
        assertEquals("July pay", state.note)
        assertEquals(TransactionType.INCOME, state.transactionType)
        assertEquals(salary, state.selectedCategory)
        assertEquals(42L, state.dateMillis)
        assertEquals(listOf(salary), state.availableCategories)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun saveInEditMode_updatesTheSameRow() = runTest {
        transactions.saveTransaction(
            transaction(id = 5, amount = 100.0, type = TransactionType.EXPENSE, note = "Lunch")
                .copy(categoryId = food.id, categoryName = "Food")
        )
        val viewModel = viewModel(transactionId = 5)
        subscribe(viewModel.uiState)

        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("180"))
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(bills))
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        val updated = transactions.current.single()
        assertEquals(5, updated.id)
        assertEquals(180.0, updated.amount, 0.001)
        assertEquals("Bills", updated.categoryName)
        assertTrue(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun editMode_unknownId_leavesFormEmpty() = runTest {
        val viewModel = viewModel(transactionId = 77)
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertTrue(state.isEditMode)
        assertEquals("", state.amount)
        assertFalse(state.isSaveEnabled)
    }
}
