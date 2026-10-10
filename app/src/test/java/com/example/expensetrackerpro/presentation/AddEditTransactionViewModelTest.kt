package com.example.expensetrackerpro.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.AddCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesByTypeUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.category.SaveCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.UpdateCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.AddTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.FormatAmountInputUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionByIdUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ResolveTransactionDateUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.SanitizeAmountInputUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.SaveTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateAmountUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateTransactionDraftUseCase
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorEvent
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorReducer
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.AddEditTransactionViewModel
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEffect
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionError
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEvent
import com.example.expensetrackerpro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Thursday 8 Oct 2026, 12:00 in Kolkata
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)
    private val timeProvider = FakeTimeProvider(now)

    private val food = category(id = 1, name = "Food", type = TransactionType.EXPENSE)
    private val bills = category(id = 2, name = "Bills", type = TransactionType.EXPENSE)
    private val salary = category(id = 3, name = "Salary", type = TransactionType.INCOME)

    private val categories = FakeCategoryRepository(listOf(food, bills, salary))
    private val transactions = FakeTransactionRepository()

    private fun viewModel(transactionId: Int? = null) = AddEditTransactionViewModel(
        getTransactionById = GetTransactionByIdUseCase(transactions),
        getCategoriesByType = GetCategoriesByTypeUseCase(categories),
        saveTransaction = SaveTransactionUseCase(
            AddTransactionUseCase(transactions),
            UpdateTransactionUseCase(transactions)
        ),
        deleteTransaction = DeleteTransactionUseCase(transactions),
        validateTransactionDraft = ValidateTransactionDraftUseCase(ValidateAmountUseCase()),
        sanitizeAmountInput = SanitizeAmountInputUseCase(),
        formatAmountInput = FormatAmountInputUseCase(),
        resolveTransactionDate = ResolveTransactionDateUseCase(timeProvider),
        getCategories = GetCategoriesUseCase(categories),
        saveCategory = SaveCategoryUseCase(
            GetCategoriesUseCase(categories),
            ValidateCategoryNameUseCase(),
            AddCategoryUseCase(categories),
            UpdateCategoryUseCase(categories)
        ),
        categoryEditorReducer = CategoryEditorReducer(ValidateCategoryNameUseCase()),
        savedStateHandle = SavedStateHandle(
            if (transactionId != null) mapOf("transactionId" to transactionId) else emptyMap()
        )
    )

    /** Everything the ViewModel sends as a one-time effect from here on. */
    private fun TestScope.effectsOf(viewModel: AddEditTransactionViewModel): List<AddEditTransactionEffect> {
        val effects = mutableListOf<AddEditTransactionEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.toList(effects)
        }
        return effects
    }

    private fun AddEditTransactionViewModel.editor(event: CategoryEditorEvent) =
        onEvent(AddEditTransactionEvent.CategoryEditor(event))

    private fun AddEditTransactionViewModel.fillValidExpense() {
        onEvent(AddEditTransactionEvent.AmountChanged("120.5"))
        onEvent(AddEditTransactionEvent.TitleChanged("Lunch"))
        onEvent(AddEditTransactionEvent.CategorySelected(food.id))
    }

    private fun storeLunch(note: String = "") {
        transactions.failWrites = false
        kotlinx.coroutines.runBlocking {
            transactions.saveTransaction(
                transaction(id = 5, amount = rupees(4201), dateMillis = 42L, note = note, title = "Lunch")
                    .copy(categoryId = food.id, categoryName = "Food")
            )
        }
    }

    // --- Add mode

    @Test
    fun addMode_startsEmptyTodayWithExpenseCategories() = runTest {
        val state = viewModel().uiState.value

        assertFalse(state.isEditMode)
        assertFalse(state.isSaveEnabled)
        assertEquals("", state.amount)
        assertEquals("", state.title)
        assertEquals("", state.note)
        assertFalse(state.isNoteExpanded)
        assertNull(state.selectedCategoryId)
        assertEquals(TransactionType.EXPENSE, state.transactionType)
        assertEquals(listOf(food, bills), state.availableCategories)
    }

    @Test
    fun defaultDate_comesFromTheTimeProvider() = runTest {
        val state = viewModel().uiState.value

        assertEquals(now, state.dateMillis)
        assertEquals(DateChoice.TODAY, state.dateChoice)
        assertEquals(
            millisAt(2026, Calendar.OCTOBER, 8, timeZone = TimeZone.getTimeZone("UTC")),
            state.maxPickerUtcMillis
        )
    }

    @Test
    fun missingTransactionIdArgument_orMinusOne_isAddMode() = runTest {
        assertFalse(viewModel(transactionId = -1).uiState.value.isEditMode)
    }

    @Test
    fun saveEnabledOnlyWithAmountTitleAndCategory() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("120"))
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onEvent(AddEditTransactionEvent.TitleChanged("Lunch"))
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(food.id))
        assertTrue(viewModel.uiState.value.isSaveEnabled)

        viewModel.onEvent(AddEditTransactionEvent.TitleChanged("  "))
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun aNoteDoesNotStandInForTheTitle() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("120"))
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(food.id))

        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("With the team"))

        assertEquals("", viewModel.uiState.value.title)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun titleError_showsOnceTheTitleIsEditedAndLeftBlank() = runTest {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.isTitleErrorVisible)

        viewModel.onEvent(AddEditTransactionEvent.TitleChanged("L"))
        assertFalse(viewModel.uiState.value.isTitleErrorVisible)

        viewModel.onEvent(AddEditTransactionEvent.TitleChanged(""))
        assertTrue(viewModel.uiState.value.isTitleErrorVisible)
    }

    // --- Amount

    @Test
    fun amountThatIsNotPlainDigits_isIgnored() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("12"))

        listOf("12a", "1e5", "NaN", "Infinity", "-12", "1,200", "12.345").forEach {
            viewModel.onEvent(AddEditTransactionEvent.AmountChanged(it))
        }

        assertEquals("12", viewModel.uiState.value.amount)
        assertEquals(rupees(12), viewModel.uiState.value.validAmount)
    }

    @Test
    fun zeroAmount_isShownButCannotBeSaved() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidExpense()

        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("0"))

        assertEquals("0", viewModel.uiState.value.amount)
        assertNull(viewModel.uiState.value.validAmount)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    // --- Note

    @Test
    fun note_expandsOnRequest_andIsCutAtTwoHundredCharacters() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(AddEditTransactionEvent.NoteExpanded)
        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("n".repeat(250)))

        assertTrue(viewModel.uiState.value.isNoteExpanded)
        assertEquals(200, viewModel.uiState.value.note.length)
    }

    @Test
    fun removeNote_clearsAndCollapsesIt() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.NoteExpanded)
        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("With the team"))

        viewModel.onEvent(AddEditTransactionEvent.NoteRemoved)

        assertEquals("", viewModel.uiState.value.note)
        assertFalse(viewModel.uiState.value.isNoteExpanded)
    }

    // --- Type and category

    @Test
    fun typeChange_clearsTheCategoryAndSwapsAvailableCategories() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidExpense()

        viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.INCOME))
        val state = viewModel.uiState.value

        assertNull(state.selectedCategoryId)
        assertFalse(state.isSaveEnabled)
        assertEquals(listOf(salary), state.availableCategories)
        // The rest of the form is kept
        assertEquals("120.5", state.amount)
        assertEquals("Lunch", state.title)
    }

    @Test
    fun selectingTheSameType_keepsTheCategory() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidExpense()

        viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.EXPENSE))

        assertEquals(food.id, viewModel.uiState.value.selectedCategoryId)
        assertTrue(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun categoriesAreSelectedById_soSharedNamesStayApart() = runTest {
        val otherFood = category(id = 9, name = "Food", type = TransactionType.EXPENSE)
        categories.insertCategory(otherFood)
        val viewModel = viewModel()

        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(otherFood.id))

        assertEquals(otherFood, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun categorySheet_opensAndDismisses() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(AddEditTransactionEvent.CategorySheetOpened)
        val editor = requireNotNull(viewModel.uiState.value.categoryEditor)

        // A new category of the type the transaction has
        assertFalse(editor.isEditMode)
        assertEquals(TransactionType.EXPENSE, editor.type)

        viewModel.editor(CategoryEditorEvent.Dismissed)
        assertNull(viewModel.uiState.value.categoryEditor)
    }

    @Test
    fun categorySheet_opensOnTheTransactionsType() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.INCOME))

        viewModel.onEvent(AddEditTransactionEvent.CategorySheetOpened)

        assertEquals(TransactionType.INCOME, viewModel.uiState.value.categoryEditor?.type)
    }

    @Test
    fun addedCategory_isStoredListedAndSelected() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.CategorySheetOpened)

        viewModel.editor(CategoryEditorEvent.NameChanged("  Gym "))
        viewModel.editor(CategoryEditorEvent.IconSelected(CategoryIcon.FITNESS_CENTER))
        viewModel.editor(CategoryEditorEvent.SaveClicked)
        val state = viewModel.uiState.value
        val gym = categories.current.single { it.name == "Gym" }

        assertNull(state.categoryEditor)
        assertEquals(listOf("Food", "Bills", "Gym"), state.availableCategories.map { it.name })
        assertTrue(gym.isCustom)
        assertEquals(CategoryIcon.FITNESS_CENTER, gym.icon)
        assertEquals(gym.id, state.selectedCategoryId)
        assertEquals(gym, state.selectedCategory)
    }

    @Test
    fun aDuplicateCategoryName_keepsTheSheetOpenWithAnError_andStoresNothing() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.CategorySheetOpened)

        viewModel.editor(CategoryEditorEvent.NameChanged("bills"))
        viewModel.editor(CategoryEditorEvent.SaveClicked)
        val editor = requireNotNull(viewModel.uiState.value.categoryEditor)

        assertEquals(CategoryNameValidation.DUPLICATE, editor.nameError)
        assertFalse(editor.isSaveEnabled)
        assertEquals(listOf(food, bills, salary), categories.current)
    }

    @Test
    fun aNameIsCheckedAgainstTheTypeChosenInTheSheet_notTheTransactions() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.CategorySheetOpened)

        // Free among expenses, taken among income
        viewModel.editor(CategoryEditorEvent.NameChanged("Salary"))
        assertNull(viewModel.uiState.value.categoryEditor?.nameError)

        viewModel.editor(CategoryEditorEvent.TypeChanged(TransactionType.INCOME))
        assertEquals(CategoryNameValidation.DUPLICATE, viewModel.uiState.value.categoryEditor?.nameError)
    }

    @Test
    fun aCategoryAddedForTheOtherType_isStoredButNotSelected() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(food.id))
        viewModel.onEvent(AddEditTransactionEvent.CategorySheetOpened)

        viewModel.editor(CategoryEditorEvent.TypeChanged(TransactionType.INCOME))
        viewModel.editor(CategoryEditorEvent.NameChanged("Bonus"))
        viewModel.editor(CategoryEditorEvent.SaveClicked)
        val state = viewModel.uiState.value

        assertEquals(TransactionType.INCOME, categories.current.single { it.name == "Bonus" }.type)
        assertNull(state.categoryEditor)
        assertEquals(food.id, state.selectedCategoryId)
        assertEquals(listOf("Food", "Bills"), state.availableCategories.map { it.name })
    }

    // --- Date

    @Test
    fun yesterdayShortcut_thenToday() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(AddEditTransactionEvent.DateChoiceSelected(DateChoice.YESTERDAY))
        assertEquals(millisAt(2026, Calendar.OCTOBER, 7, hour = 12), viewModel.uiState.value.dateMillis)
        assertEquals(DateChoice.YESTERDAY, viewModel.uiState.value.dateChoice)

        viewModel.onEvent(AddEditTransactionEvent.DateChoiceSelected(DateChoice.TODAY))
        assertEquals(now, viewModel.uiState.value.dateMillis)
        assertEquals(DateChoice.TODAY, viewModel.uiState.value.dateChoice)
    }

    @Test
    fun pickedDate_isStoredAsThatLocalDay_andFutureDatesAreNot() = runTest {
        val utc = TimeZone.getTimeZone("UTC")
        val viewModel = viewModel()

        viewModel.onEvent(
            AddEditTransactionEvent.DatePicked(millisAt(2026, Calendar.SEPTEMBER, 20, timeZone = utc))
        )
        assertEquals(millisAt(2026, Calendar.SEPTEMBER, 20, hour = 12), viewModel.uiState.value.dateMillis)
        assertEquals(DateChoice.OTHER, viewModel.uiState.value.dateChoice)

        viewModel.onEvent(
            AddEditTransactionEvent.DatePicked(millisAt(2026, Calendar.DECEMBER, 25, timeZone = utc))
        )
        assertEquals(now, viewModel.uiState.value.dateMillis)
    }

    // --- Save

    @Test
    fun saveInAddMode_storesTitleAndNoteSeparately_thenNavigatesBack() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        viewModel.fillValidExpense()
        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("  With the team  "))
        viewModel.onEvent(AddEditTransactionEvent.DateChoiceSelected(DateChoice.YESTERDAY))

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        val saved = transactions.current.single()
        assertEquals(0, saved.id)
        assertEquals(Money(120_50), saved.amount)
        assertEquals("Lunch", saved.title)
        assertEquals("With the team", saved.note)
        assertEquals(food.id, saved.categoryId)
        assertEquals("Food", saved.categoryName)
        assertEquals(TransactionType.EXPENSE, saved.transactionType)
        assertEquals(millisAt(2026, Calendar.OCTOBER, 7, hour = 12), saved.dateMillis)
        assertEquals(listOf<AddEditTransactionEffect>(AddEditTransactionEffect.NavigateBack), effects)
    }

    @Test
    fun saveWithoutANote_storesAnEmptyNote() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidExpense()

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertEquals("", transactions.current.single().note)
    }

    @Test
    fun saveWhileInvalid_storesNothingAndStays() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("120"))

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertTrue(transactions.current.isEmpty())
        assertTrue(effects.isEmpty())
    }

    @Test
    fun secondSaveTap_doesNotStoreTwice() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidExpense()

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertEquals(1, transactions.current.size)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun failedSave_showsAnErrorAndKeepsTheFormForAnotherTry() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        viewModel.fillValidExpense()
        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("With the team"))
        transactions.failWrites = true

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)
        val state = viewModel.uiState.value

        assertEquals(
            listOf<AddEditTransactionEffect>(
                AddEditTransactionEffect.ShowError(AddEditTransactionError.SAVE_FAILED)
            ),
            effects
        )
        assertTrue(transactions.current.isEmpty())
        assertEquals("120.5", state.amount)
        assertEquals("Lunch", state.title)
        assertEquals("With the team", state.note)
        assertEquals(food.id, state.selectedCategoryId)
        assertFalse(state.isSaving)
        assertTrue(state.isSaveEnabled)

        transactions.failWrites = false
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertEquals(1, transactions.current.size)
        assertEquals(AddEditTransactionEffect.NavigateBack, effects.last())
    }

    @Test
    fun effectSentBeforeAnyoneCollects_isStillDelivered() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidExpense()
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertEquals(listOf<AddEditTransactionEffect>(AddEditTransactionEffect.NavigateBack), effectsOf(viewModel))
    }

    // --- Edit mode

    @Test
    fun editMode_prefillsFromTheStoredTransaction() = runTest {
        transactions.saveTransaction(
            transaction(id = 5, amount = rupees(5000), type = TransactionType.INCOME, dateMillis = 42L, note = "", title = "July pay")
                .copy(categoryId = salary.id, categoryName = "Salary")
        )

        val state = viewModel(transactionId = 5).uiState.value

        assertTrue(state.isEditMode)
        assertEquals("5000", state.amount)
        assertEquals("July pay", state.title)
        assertEquals("", state.note)
        assertFalse(state.isNoteExpanded)
        assertEquals(TransactionType.INCOME, state.transactionType)
        assertEquals(salary, state.selectedCategory)
        assertEquals(42L, state.dateMillis)
        assertEquals(DateChoice.OTHER, state.dateChoice)
        assertEquals(listOf(salary), state.availableCategories)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun editMode_showsAmountsWithoutATrailingZero() = runTest {
        storeLunch()
        assertEquals("4201", viewModel(transactionId = 5).uiState.value.amount)

        transactions.updateTransaction(transactions.current.single().copy(amount = Money(4201_50)))
        assertEquals("4201.5", viewModel(transactionId = 5).uiState.value.amount)
    }

    @Test
    fun editMode_withANote_showsItExpanded() = runTest {
        storeLunch(note = "With the team")

        val state = viewModel(transactionId = 5).uiState.value

        assertEquals("With the team", state.note)
        assertTrue(state.isNoteExpanded)
    }

    @Test
    fun saveInEditMode_updatesTheSameRow_thenNavigatesBack() = runTest {
        storeLunch(note = "With the team")
        val viewModel = viewModel(transactionId = 5)
        val effects = effectsOf(viewModel)

        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("180"))
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(bills.id))
        viewModel.onEvent(AddEditTransactionEvent.NoteRemoved)
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        val updated = transactions.current.single()
        assertEquals(5, updated.id)
        assertEquals(rupees(180), updated.amount)
        assertEquals("Lunch", updated.title)
        assertEquals("", updated.note)
        assertEquals("Bills", updated.categoryName)
        assertEquals(42L, updated.dateMillis)
        assertEquals(listOf<AddEditTransactionEffect>(AddEditTransactionEffect.NavigateBack), effects)
    }

    @Test
    fun editMode_unknownId_leavesTheFormEmpty() = runTest {
        val state = viewModel(transactionId = 77).uiState.value

        assertTrue(state.isEditMode)
        assertEquals("", state.amount)
        assertFalse(state.isSaveEnabled)
    }

    // --- Delete

    @Test
    fun delete_asksFirst_andDismissKeepsTheTransaction() = runTest {
        storeLunch()
        val viewModel = viewModel(transactionId = 5)
        val effects = effectsOf(viewModel)

        viewModel.onEvent(AddEditTransactionEvent.DeleteClicked)
        assertEquals(transactions.current.single(), viewModel.uiState.value.transactionToDelete)

        viewModel.onEvent(AddEditTransactionEvent.DeleteDismissed)

        assertNull(viewModel.uiState.value.transactionToDelete)
        assertEquals(1, transactions.current.size)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun deleteConfirmed_removesTheTransaction_thenNavigatesBack() = runTest {
        storeLunch()
        val viewModel = viewModel(transactionId = 5)
        val effects = effectsOf(viewModel)

        viewModel.onEvent(AddEditTransactionEvent.DeleteClicked)
        viewModel.onEvent(AddEditTransactionEvent.DeleteConfirmed)

        assertTrue(transactions.current.isEmpty())
        assertNull(viewModel.uiState.value.transactionToDelete)
        assertEquals(listOf<AddEditTransactionEffect>(AddEditTransactionEffect.NavigateBack), effects)
    }

    @Test
    fun failedDelete_showsAnErrorAndStays() = runTest {
        storeLunch()
        val viewModel = viewModel(transactionId = 5)
        val effects = effectsOf(viewModel)
        transactions.failWrites = true

        viewModel.onEvent(AddEditTransactionEvent.DeleteClicked)
        viewModel.onEvent(AddEditTransactionEvent.DeleteConfirmed)

        assertEquals(1, transactions.current.size)
        assertEquals(
            listOf<AddEditTransactionEffect>(
                AddEditTransactionEffect.ShowError(AddEditTransactionError.DELETE_FAILED)
            ),
            effects
        )
        assertEquals("Lunch", viewModel.uiState.value.title)
    }

    @Test
    fun deleteInAddMode_orWithoutAsking_doesNothing() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.onEvent(AddEditTransactionEvent.DeleteClicked)
        viewModel.onEvent(AddEditTransactionEvent.DeleteConfirmed)

        assertNull(viewModel.uiState.value.transactionToDelete)
        assertTrue(effects.isEmpty())
    }
}
