package com.example.expensetrackerpro.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEntryMode
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.ReviewPosition
import com.example.expensetrackerpro.presentation.navigation.Screens
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.QuickAddField
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
import com.example.expensetrackerpro.domain.usecase.transaction.SaveTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateAmountUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateTransactionDraftUseCase
import com.example.expensetrackerpro.fakes.FakeBillTextReader
import com.example.expensetrackerpro.fakes.FakeCategoryMemoryRepository
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeQuickAddPreferencesRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.FakeVoiceInputAvailability
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.draftAssistController
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.recognizedText
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.sanitizeAmountInput
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorReducer
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.AddEditTransactionViewModel
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEffect
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEvent
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftField
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftSource
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddEvent
import com.example.expensetrackerpro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.Locale

/**
 * The Add form's quick ways in: a frequent transaction in one tap, several transactions in one
 * sentence (saved all at once, or reviewed one by one), and the mode a shortcut opens it in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionQuickAddExtrasTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Thursday 8 Oct 2026, 12:00 in Kolkata
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)
    private val timeProvider = FakeTimeProvider(now)

    private val dining = builtIn(1, "Food & Dining", BuiltInCategory.FOOD_DINING)
    private val groceries = builtIn(2, "Groceries", BuiltInCategory.GROCERIES)
    private val shopping = builtIn(3, "Shopping", BuiltInCategory.SHOPPING)
    private val bills = builtIn(4, "Bills", BuiltInCategory.BILLS_UTILITIES)
    private val salary = builtIn(5, "Salary", BuiltInCategory.SALARY)

    private fun builtIn(id: Int, name: String, key: BuiltInCategory) =
        category(id = id, name = name, type = key.type).copy(builtInKey = key)

    private val categories = FakeCategoryRepository(listOf(dining, groceries, shopping, bills, salary))
    private val transactions = FakeTransactionRepository()
    private val billReader = FakeBillTextReader()
    private val memory = FakeCategoryMemoryRepository()
    private val preferences = FakeQuickAddPreferencesRepository()
    private val voice = FakeVoiceInputAvailability()

    private fun viewModel(
        transactionId: Int? = null,
        locale: Locale = Locale.ENGLISH,
        savedState: Map<String, Any?> = emptyMap(),
        handle: SavedStateHandle = SavedStateHandle(
            savedState + if (transactionId != null) mapOf("transactionId" to transactionId) else emptyMap()
        )
    ) = AddEditTransactionViewModel(
        getTransactionById = GetTransactionByIdUseCase(transactions),
        getCategoriesByType = GetCategoriesByTypeUseCase(categories),
        saveTransaction = SaveTransactionUseCase(AddTransactionUseCase(transactions), UpdateTransactionUseCase(transactions)),
        deleteTransaction = DeleteTransactionUseCase(transactions),
        validateTransactionDraft = ValidateTransactionDraftUseCase(ValidateAmountUseCase()),
        sanitizeAmountInput = sanitizeAmountInput(),
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
        assist = draftAssistController(
            timeProvider = timeProvider,
            billReader = billReader,
            memory = memory,
            preferences = preferences,
            voice = voice,
            locale = locale,
            transactions = transactions
        ),
        savedStateHandle = handle
    )

    private fun TestScope.effectsOf(viewModel: AddEditTransactionViewModel): List<AddEditTransactionEffect> {
        val effects = mutableListOf<AddEditTransactionEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(effects) }
        return effects
    }

    private fun AddEditTransactionViewModel.quickAdd(event: QuickAddEvent) =
        onEvent(AddEditTransactionEvent.QuickAdd(event))

    /** Types [text] and waits for the typing pause, as the preview does. */
    private fun TestScope.type(viewModel: AddEditTransactionViewModel, text: String) {
        viewModel.quickAdd(QuickAddEvent.TextChanged(text))
        advanceTimeBy(TYPING_PAUSE + 1)
        runCurrent()
    }

    private val AddEditTransactionViewModel.state get() = uiState.value

    private var nextId = 100

    private fun spend(title: String, amount: Money = rupees(100), daysAgo: Int = 1, categoryId: CategoryId = dining.id) =
        transaction(id = nextId++, amount = amount, dateMillis = now - daysAgo * DAY_MILLIS, title = title)
            .copy(categoryId = categoryId, categoryName = "")

    private suspend fun repeatSpend(title: String, times: Int, amount: Money = rupees(100), categoryId: CategoryId = dining.id) =
        repeat(times) { transactions.saveTransaction(spend(title, amount, daysAgo = it + 1, categoryId = categoryId)) }

    // --- Frequent transactions

    @Test
    fun frequentTransactions_ofTheFormsType_areOffered() = runTest {
        repeatSpend("Swiggy", 3, rupees(450))
        repeatSpend("Swiggy", 2, rupees(450), categoryId = groceries.id)
        repeatSpend("Acme payroll", 3, rupees(50_000), categoryId = salary.id)
        transactions.saveTransaction(
            spend("Acme payroll", categoryId = salary.id)
        )
        // The repository's fakes are typed by transaction: make the salary ones income
        transactions.current.filter { it.categoryId == salary.id }.forEach {
            transactions.updateTransaction(it.copy(transactionType = TransactionType.INCOME))
        }
        val viewModel = viewModel()
        runCurrent()

        assertEquals(listOf("Swiggy"), viewModel.state.quickAdd.frequent.map { it.title })
        assertEquals(rupees(450), viewModel.state.quickAdd.frequent.single().usualAmount)

        viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.INCOME))
        runCurrent()
        assertEquals(listOf("Acme payroll"), viewModel.state.quickAdd.frequent.map { it.title })
        assertEquals(4, viewModel.state.quickAdd.frequent.single().count)
    }

    @Test
    fun noRepeats_noneOffered_andNoneWhileEditing() = runTest {
        repeatSpend("Swiggy", 2)
        assertEquals(emptyList<Any>(), viewModel().also { runCurrent() }.state.quickAdd.frequent)

        repeatSpend("Uber", 3)
        val stored = transactions.current.first()
        assertEquals(emptyList<Any>(), viewModel(transactionId = stored.id.value).also { runCurrent() }.state.quickAdd.frequent)
    }

    @Test
    fun tappingAFrequentOne_fillsTheForm_datedToday_fromYourHistory() = runTest {
        repeatSpend("Swiggy", 3, rupees(450))
        val viewModel = viewModel()
        runCurrent()

        viewModel.quickAdd(QuickAddEvent.FrequentSelected(0))

        val state = viewModel.state
        assertEquals("450", state.amount)
        assertEquals("Swiggy", state.title)
        assertEquals(dining.id, state.selectedCategoryId)
        assertEquals(DateChoice.TODAY, state.dateChoice)
        assertEquals(DraftSource.History("Swiggy"), state.draftFill?.source)
        assertEquals(setOf(DraftField.AMOUNT, DraftField.TITLE, DraftField.CATEGORY), state.draftFill?.filledFields)
        assertTrue(state.isSaveEnabled)
        // A tap fills the form; only Save stores it
        assertEquals(3, transactions.current.size)
        assertTrue(preferences.used.value)
    }

    @Test
    fun aFrequentOne_neverReplacesWhatTheUserTyped() = runTest {
        repeatSpend("Swiggy", 3, rupees(450))
        val viewModel = viewModel()
        runCurrent()
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("520"))

        viewModel.quickAdd(QuickAddEvent.FrequentSelected(0))

        assertEquals("520", viewModel.state.amount)
        assertEquals("Swiggy", viewModel.state.title)
    }

    // --- Several in one sentence

    @Test
    fun aSentenceWithTwoAmounts_showsABatch_insteadOfOnePreview() = runTest {
        val viewModel = viewModel()

        type(viewModel, "450 swiggy and 300 dmart yesterday")

        val batch = viewModel.state.quickAdd.batch
        assertNull(viewModel.state.quickAdd.preview)
        assertEquals(listOf(rupees(450), rupees(300)), batch?.rows?.map { it.amount })
        assertEquals(listOf(dining, groceries), batch?.rows?.map { it.category })
        assertEquals(DateChoice.YESTERDAY, batch?.sharedDate?.choice)
        assertEquals(rupees(750), batch?.total)
        assertEquals(true, batch?.canSaveAll)
        // Nothing in the form has changed
        assertEquals("", viewModel.state.amount)
    }

    @Test
    fun aRowWithoutACategory_canBeGivenOne_beforeSaveAll() = runTest {
        val viewModel = viewModel()
        type(viewModel, "450 swiggy and 120 uber")
        assertEquals(false, viewModel.state.quickAdd.batch?.canSaveAll)

        viewModel.quickAdd(QuickAddEvent.BatchCategoryClicked(1))
        assertEquals(QuickAddField.CATEGORY, viewModel.state.quickAdd.editing)
        assertEquals(1, viewModel.state.quickAdd.editingRow)

        viewModel.quickAdd(QuickAddEvent.BatchCategoryChosen(1, shopping.id))

        assertEquals(listOf(dining, shopping), viewModel.state.quickAdd.batch?.rows?.map { it.category })
        assertEquals(true, viewModel.state.quickAdd.batch?.canSaveAll)
        assertNull(viewModel.state.quickAdd.editing)
    }

    @Test
    fun saveAll_storesEveryOne_andClosesTheForm() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        type(viewModel, "450 swiggy and 300 dmart yesterday")

        viewModel.quickAdd(QuickAddEvent.SaveAllClicked)
        runCurrent()

        assertEquals(listOf("Swiggy", "Dmart"), transactions.current.map { it.title })
        assertEquals(listOf(rupees(450), rupees(300)), transactions.current.map { it.amount })
        assertEquals(listOf(dining.id, groceries.id), transactions.current.map { it.categoryId })
        val yesterday = millisAt(2026, Calendar.OCTOBER, 7)
        assertTrue(transactions.current.all { it.dateMillis in yesterday until yesterday + DAY_MILLIS })
        assertEquals(listOf(AddEditTransactionEffect.Saved), effects)
        assertEquals(1, transactions.batchSaveCalls)
    }

    @Test
    fun saveAll_thatFails_storesNothing_andKeepsTheBatch() = runTest {
        transactions.failWrites = true
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        type(viewModel, "450 swiggy and 300 dmart")

        viewModel.quickAdd(QuickAddEvent.SaveAllClicked)
        runCurrent()

        assertEquals(emptyList<Any>(), transactions.current)
        assertEquals(listOf(AddEditTransactionEffect.ShowError(R.string.error_storage_failure)), effects)
        assertFalse(viewModel.state.quickAdd.isSavingAll)
        assertEquals(2, viewModel.state.quickAdd.batch?.rows?.size)
    }

    @Test
    fun saveAll_remembersTheCategoriesTheUserChanged_andOnlyThose() = runTest {
        val viewModel = viewModel()
        type(viewModel, "450 swiggy and 120 uber and 300 dmart")
        // Uber had none, so picking one is a correction; Dmart's suggestion is picked again, which is not
        viewModel.quickAdd(QuickAddEvent.BatchCategoryChosen(1, shopping.id))
        viewModel.quickAdd(QuickAddEvent.BatchCategoryChosen(2, groceries.id))

        viewModel.quickAdd(QuickAddEvent.SaveAllClicked)
        runCurrent()

        assertEquals(3, transactions.current.size)
        assertEquals(listOf(CategoryMemory("uber", TransactionType.EXPENSE, shopping.id)), memory.memory.value)
    }

    @Test
    fun saveAll_changingASuggestedCategory_isRemembered() = runTest {
        val viewModel = viewModel()
        type(viewModel, "450 swiggy and 300 dmart")
        viewModel.quickAdd(QuickAddEvent.BatchCategoryChosen(0, shopping.id))

        viewModel.quickAdd(QuickAddEvent.SaveAllClicked)
        runCurrent()

        assertEquals(listOf(CategoryMemory("swiggy", TransactionType.EXPENSE, shopping.id)), memory.memory.value)
    }

    @Test
    fun saveAll_thatFails_remembersNothing_andAMemoryThatFails_stillSaves() = runTest {
        transactions.failWrites = true
        val failing = viewModel()
        type(failing, "450 swiggy and 120 uber")
        failing.quickAdd(QuickAddEvent.BatchCategoryChosen(1, shopping.id))
        failing.quickAdd(QuickAddEvent.SaveAllClicked)
        runCurrent()
        assertEquals(emptyList<Any>(), memory.memory.value)

        transactions.failWrites = false
        memory.failWith = DomainError.StorageFailure()
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        type(viewModel, "450 swiggy and 120 uber")
        viewModel.quickAdd(QuickAddEvent.BatchCategoryChosen(1, shopping.id))
        viewModel.quickAdd(QuickAddEvent.SaveAllClicked)
        runCurrent()

        assertEquals(2, transactions.current.size)
        assertEquals(listOf(AddEditTransactionEffect.Saved), effects)
    }

    @Test
    fun saveAll_isNotOffered_whileARowIsIncomplete() = runTest {
        val viewModel = viewModel()
        type(viewModel, "450 swiggy and 120 uber")

        viewModel.quickAdd(QuickAddEvent.SaveAllClicked)
        runCurrent()

        assertEquals(emptyList<Any>(), transactions.current)
        assertEquals(0, transactions.batchSaveCalls)
    }

    @Test
    fun reviewOneByOne_fillsTheFormWithEach_andClosesAfterTheLast() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        type(viewModel, "450 swiggy and 120 uber")

        viewModel.quickAdd(QuickAddEvent.ReviewOneByOneClicked)

        assertEquals("450", viewModel.state.amount)
        assertEquals("Swiggy", viewModel.state.title)
        assertEquals(ReviewPosition(1, 2), viewModel.state.quickAdd.reviewPosition)
        assertEquals(DraftSource.Text("450 swiggy"), viewModel.state.draftFill?.source)
        assertEquals("", viewModel.state.quickAdd.text)

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)
        runCurrent()

        assertEquals(listOf("Swiggy"), transactions.current.map { it.title })
        assertEquals(listOf(AddEditTransactionEffect.SavedOneOfMany(1, 2)), effects)
        assertEquals("120", viewModel.state.amount)
        assertEquals("Uber", viewModel.state.title)
        // Uber has no category of its own here: the user picks one, as with any sentence
        assertNull(viewModel.state.selectedCategoryId)
        assertEquals(ReviewPosition(2, 2), viewModel.state.quickAdd.reviewPosition)
        assertFalse(viewModel.state.isSaving)

        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(shopping.id))
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)
        runCurrent()

        assertEquals(listOf("Swiggy", "Uber"), transactions.current.map { it.title })
        assertEquals(AddEditTransactionEffect.Saved, effects.last())
    }

    @Test
    fun reviewOneByOne_keepsEachPartsType() = runTest {
        val viewModel = viewModel()
        type(viewModel, "50000 salary and 450 swiggy")

        viewModel.quickAdd(QuickAddEvent.ReviewOneByOneClicked)
        assertEquals(TransactionType.INCOME, viewModel.state.transactionType)
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)
        runCurrent()

        assertEquals(TransactionType.EXPENSE, viewModel.state.transactionType)
        assertEquals(dining.id, viewModel.state.selectedCategoryId)
    }

    @Test
    fun clearingTheBanner_stopsTheReview() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        type(viewModel, "450 swiggy and 300 dmart")
        viewModel.quickAdd(QuickAddEvent.ReviewOneByOneClicked)

        viewModel.onEvent(AddEditTransactionEvent.BillFillCleared)
        assertNull(viewModel.state.quickAdd.reviewPosition)
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("80"))
        viewModel.onEvent(AddEditTransactionEvent.TitleChanged("Chai"))
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(dining.id))
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)
        runCurrent()

        assertEquals(listOf(AddEditTransactionEffect.Saved), effects)
    }

    // --- The mode a shortcut opens the form in

    @Test
    fun eachMode_asksTheScreenForWhatItOpens() = runTest {
        assertNull(viewModel().state.quickAdd.launchRequest)
        assertNull(viewModel(savedState = mapOf("mode" to "MANUAL")).state.quickAdd.launchRequest)
        assertEquals(AddEntryMode.QUICK_ADD, viewModel(savedState = mapOf("mode" to "QUICK_ADD")).state.quickAdd.launchRequest)
        assertEquals(AddEntryMode.SCAN, viewModel(savedState = mapOf("mode" to "SCAN")).state.quickAdd.launchRequest)
        assertEquals(AddEntryMode.VOICE, viewModel(savedState = mapOf("mode" to "voice")).state.quickAdd.launchRequest)
        // A mode this version doesn't know opens the plain form
        assertNull(viewModel(savedState = mapOf("mode" to "TELEPATHY")).state.quickAdd.launchRequest)
    }

    @Test
    fun withoutAVoiceRecognizer_voiceOpensTheBarInstead() = runTest {
        voice.available = false

        assertEquals(AddEntryMode.QUICK_ADD, viewModel(savedState = mapOf("mode" to "VOICE")).state.quickAdd.launchRequest)
    }

    @Test
    fun aModeIsHandledOnce_evenWhenTheScreenIsMadeAgain() = runTest {
        val savedState = mutableMapOf<String, Any?>("mode" to "QUICK_ADD")
        val handle = SavedStateHandle(savedState)
        val viewModel = viewModel(handle = handle)

        viewModel.quickAdd(QuickAddEvent.LaunchHandled)

        assertNull(viewModel.state.quickAdd.launchRequest)
        // The process came back with what was saved: nothing is opened again
        val restored = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })
        assertNull(viewModel(handle = restored).state.quickAdd.launchRequest)
    }

    @Test
    fun aStoredTransaction_isNeverOpenedInAMode() = runTest {
        transactions.saveTransaction(transaction(id = 5, title = "Lunch"))

        assertNull(viewModel(transactionId = 5, savedState = mapOf("mode" to "SCAN")).state.quickAdd.launchRequest)
    }

    @Test
    fun theRoute_carriesTheMode_exceptForThePlainForm() {
        assertEquals("transaction_form", Screens.TransactionForm.newRoute(AddEntryMode.MANUAL))
        assertEquals("transaction_form?mode=SCAN", Screens.TransactionForm.newRoute(AddEntryMode.SCAN))
        assertEquals(AddEntryMode.SCAN, AddEntryMode.fromName(AddEntryMode.SCAN.name))
        assertEquals(AddEntryMode.MANUAL, AddEntryMode.fromName(null))
    }

    private companion object {
        const val TYPING_PAUSE = 300L
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
