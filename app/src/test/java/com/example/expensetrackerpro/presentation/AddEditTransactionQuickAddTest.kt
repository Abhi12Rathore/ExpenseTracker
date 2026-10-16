package com.example.expensetrackerpro.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.DateChoice
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

/** Typing or saying a sentence into the Add transaction form, and what the form learns from corrections. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionQuickAddTest {

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
        savedState: Map<String, Any?> = emptyMap()
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
        savedStateHandle = SavedStateHandle(
            savedState + if (transactionId != null) mapOf("transactionId" to transactionId) else emptyMap()
        )
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

    // --- The bar

    @Test
    fun theBar_isOfferedOnANewTransaction_withTheExamples_andNotWhileEditing() = runTest {
        transactions.saveTransaction(transaction(id = 5, title = "Lunch"))

        val add = viewModel().state.quickAdd
        assertTrue(add.isAvailable)
        assertTrue(add.showExamples)
        assertTrue(add.isVoiceAvailable)

        assertFalse(viewModel(transactionId = 5).state.quickAdd.isAvailable)
    }

    @Test
    fun theMicrophone_listensInTheAppsLanguage_andGoesWithoutARecognizer() = runTest {
        assertEquals("en-IN", viewModel().state.quickAdd.voiceLanguageTag)
        assertEquals("hi", viewModel(locale = Locale.forLanguageTag("hi")).state.quickAdd.voiceLanguageTag)

        voice.available = false
        assertFalse(viewModel().state.quickAdd.isVoiceAvailable)

        voice.available = true
        val viewModel = viewModel()
        viewModel.quickAdd(QuickAddEvent.VoiceUnavailable)
        assertFalse(viewModel.state.quickAdd.isVoiceAvailable)
    }

    // --- The preview

    @Test
    fun thePreview_waitsForAPauseInTyping() = runTest {
        val viewModel = viewModel()

        viewModel.quickAdd(QuickAddEvent.TextChanged("450 swiggy yesterday"))
        advanceTimeBy(TYPING_PAUSE - 50)
        runCurrent()
        assertNull(viewModel.state.quickAdd.preview)

        advanceTimeBy(51)
        runCurrent()
        val preview = viewModel.state.quickAdd.preview
        assertEquals(rupees(450), preview?.amount)
        assertEquals(FieldConfidence.SURE, preview?.amountConfidence)
        assertEquals(dining, preview?.category)
        assertEquals(FieldConfidence.GUESS, preview?.categoryConfidence)
        assertEquals("Swiggy", preview?.title)
        assertEquals(DateChoice.YESTERDAY, preview?.date?.choice)
        // Nothing in the form has changed yet
        assertEquals("", viewModel.state.amount)
    }

    @Test
    fun anExample_orWhatWasHeard_isReadAtOnce() = runTest {
        val viewModel = viewModel()

        viewModel.quickAdd(QuickAddEvent.ExampleSelected("kal 200 ka petrol"))
        assertEquals("kal 200 ka petrol", viewModel.state.quickAdd.text)
        assertEquals(rupees(200), viewModel.state.quickAdd.preview?.amount)

        viewModel.quickAdd(QuickAddEvent.VoiceRecognized("got my salary one lakh five thousand today"))
        assertEquals(rupees(105_000), viewModel.state.quickAdd.preview?.amount)
        assertEquals(TransactionType.INCOME, viewModel.state.quickAdd.preview?.type)
        assertEquals(salary, viewModel.state.quickAdd.preview?.category)
    }

    @Test
    fun emptyingTheBar_takesThePreviewAway() = runTest {
        val viewModel = viewModel()
        type(viewModel, "450 swiggy")

        viewModel.quickAdd(QuickAddEvent.TextChanged(""))

        assertNull(viewModel.state.quickAdd.preview)
        assertFalse(viewModel.state.quickAdd.canFill)
    }

    @Test
    fun theCategoryChip_offersTheTypesCategories_andWhatIsPickedIsSure() = runTest {
        val viewModel = viewModel()
        type(viewModel, "dmart shopping yesterday")

        viewModel.quickAdd(QuickAddEvent.ChipClicked(QuickAddField.CATEGORY))
        assertEquals(QuickAddField.CATEGORY, viewModel.state.quickAdd.editing)
        assertEquals(listOf(dining, groceries, shopping, bills), viewModel.state.quickAdd.preview?.categoryOptions)

        viewModel.quickAdd(QuickAddEvent.CategoryChosen(shopping.id))

        assertNull(viewModel.state.quickAdd.editing)
        assertEquals(shopping, viewModel.state.quickAdd.preview?.category)
        assertEquals(FieldConfidence.SURE, viewModel.state.quickAdd.preview?.categoryConfidence)

        // Kept while the text is edited
        type(viewModel, "dmart shopping yesterday 1240")
        assertEquals(shopping, viewModel.state.quickAdd.preview?.category)
    }

    @Test
    fun theDateChip_takesTodayYesterdayOrAPickedDay() = runTest {
        val viewModel = viewModel()
        type(viewModel, "450 swiggy yesterday")

        viewModel.quickAdd(QuickAddEvent.ChipClicked(QuickAddField.DATE))
        assertEquals(QuickAddField.DATE, viewModel.state.quickAdd.editing)
        viewModel.quickAdd(QuickAddEvent.DateChoiceChosen(DateChoice.TODAY))
        assertEquals(DateChoice.TODAY, viewModel.state.quickAdd.preview?.date?.choice)

        viewModel.quickAdd(QuickAddEvent.DatePicked(com.example.expensetrackerpro.fakes.utcDay(2026, Calendar.OCTOBER, 2)))
        assertEquals(DateChoice.OTHER, viewModel.state.quickAdd.preview?.date?.choice)
        assertEquals(millisAt(2026, Calendar.OCTOBER, 2, hour = 12), viewModel.state.quickAdd.preview?.date?.millis)
    }

    // --- Filling the form

    @Test
    fun fill_putsTheTextIntoTheForm_marksWhatItFilled_andEmptiesTheBar() = runTest {
        val viewModel = viewModel()
        type(viewModel, "Spent 450 on swiggy yesterday, dinner with Rahul")

        viewModel.quickAdd(QuickAddEvent.FillClicked)

        val state = viewModel.state
        assertEquals("450", state.amount)
        assertEquals("Swiggy", state.title)
        assertEquals(dining.id, state.selectedCategoryId)
        assertEquals("dinner with Rahul", state.note)
        assertEquals(DateChoice.YESTERDAY, state.dateChoice)
        assertTrue(state.isSaveEnabled)
        assertEquals(DraftSource.Text("Spent 450 on swiggy yesterday, dinner with Rahul"), state.draftFill?.source)
        assertEquals(
            setOf(DraftField.AMOUNT, DraftField.TITLE, DraftField.CATEGORY, DraftField.NOTE, DraftField.DATE),
            state.draftFill?.filledFields
        )
        assertEquals("", state.quickAdd.text)
        assertNull(state.quickAdd.preview)
        // Nothing is saved without Save
        assertTrue(transactions.current.isEmpty())
    }

    @Test
    fun fill_neverReplacesWhatTheUserTyped() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("999"))
        type(viewModel, "450 swiggy")

        viewModel.quickAdd(QuickAddEvent.FillClicked)

        assertEquals("999", viewModel.state.amount)
        assertEquals("Swiggy", viewModel.state.title)
        assertFalse(viewModel.state.isFromDraft(DraftField.AMOUNT))
    }

    @Test
    fun fill_withNoAmount_bringsUpTheKeypad_andTheFirstUseHidesTheExamples() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        type(viewModel, "dmart shopping yesterday")

        viewModel.quickAdd(QuickAddEvent.FillClicked)

        assertEquals(listOf(AddEditTransactionEffect.FocusAmount), effects)
        assertTrue(viewModel.state.draftFill?.isAmountMissing ?: false)
        assertTrue(preferences.used.value)
        assertFalse(viewModel.state.quickAdd.showExamples)
    }

    @Test
    fun theAmountChip_fillsTheForm_andBringsUpTheKeypad() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        type(viewModel, "450 swiggy")

        viewModel.quickAdd(QuickAddEvent.ChipClicked(QuickAddField.AMOUNT))

        assertEquals("450", viewModel.state.amount)
        assertEquals(listOf(AddEditTransactionEffect.FocusAmount), effects)
    }

    @Test
    fun anIncomeWord_switchesTheForm_andClearPutsItBack() = runTest {
        val viewModel = viewModel()
        type(viewModel, "salary 1,05,000")

        viewModel.quickAdd(QuickAddEvent.FillClicked)
        assertEquals(TransactionType.INCOME, viewModel.state.transactionType)
        assertEquals(salary.id, viewModel.state.selectedCategoryId)
        assertTrue(viewModel.state.isSaveEnabled)

        viewModel.onEvent(AddEditTransactionEvent.BillFillCleared)

        assertEquals(TransactionType.EXPENSE, viewModel.state.transactionType)
        assertEquals("", viewModel.state.amount)
        assertEquals("", viewModel.state.title)
        assertNull(viewModel.state.selectedCategoryId)
        assertNull(viewModel.state.draftFill)
    }

    @Test
    fun withoutAWordForIncomeOrExpense_theFormKeepsItsType() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.INCOME))
        type(viewModel, "1000 from dad")

        assertEquals(TransactionType.INCOME, viewModel.state.quickAdd.preview?.type)
        viewModel.quickAdd(QuickAddEvent.FillClicked)

        assertEquals(TransactionType.INCOME, viewModel.state.transactionType)
        assertEquals("Dad", viewModel.state.title)
    }

    // --- Learning from corrections

    /** "dmart shopping yesterday" filled in, with the amount typed after. Groceries is suggested. */
    private fun TestScope.filledDmart(viewModel: AddEditTransactionViewModel) {
        type(viewModel, "dmart shopping yesterday")
        viewModel.quickAdd(QuickAddEvent.FillClicked)
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("1240"))
    }

    @Test
    fun aCorrectedCategory_isAnnounced_andRememberedOnSave_thenSureNextTime() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        filledDmart(viewModel)
        assertEquals(groceries.id, viewModel.state.selectedCategoryId)

        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(shopping.id))
        assertTrue(AddEditTransactionEffect.CategoryLearned("Dmart", "Shopping") in effects)
        // Not before Save
        assertTrue(memory.memory.value.isEmpty())

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)
        assertEquals(listOf(CategoryMemory("dmart", TransactionType.EXPENSE, shopping.id)), memory.memory.value)

        val next = viewModel()
        type(next, "dmart 1240")
        assertEquals(shopping, next.state.quickAdd.preview?.category)
        assertEquals(FieldConfidence.SURE, next.state.quickAdd.preview?.categoryConfidence)
    }

    @Test
    fun undo_meansTheCorrectionIsNotRemembered() = runTest {
        val viewModel = viewModel()
        filledDmart(viewModel)
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(shopping.id))

        viewModel.quickAdd(QuickAddEvent.LearnUndone)
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertTrue(transactions.current.isNotEmpty())
        assertTrue(memory.memory.value.isEmpty())
    }

    @Test
    fun aCategoryPickedWhereNoneWasFound_isLearnedToo() = runTest {
        val viewModel = viewModel()
        type(viewModel, "zxqv 300")
        viewModel.quickAdd(QuickAddEvent.FillClicked)
        assertNull(viewModel.state.selectedCategoryId)

        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(bills.id))
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertEquals(listOf(CategoryMemory("zxqv", TransactionType.EXPENSE, bills.id)), memory.memory.value)
    }

    @Test
    fun aTitleCorrectedBefore_isUpdatedQuietly() = runTest {
        memory.memory.value = listOf(CategoryMemory("dmart", TransactionType.EXPENSE, bills.id))
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        filledDmart(viewModel)
        assertEquals(bills.id, viewModel.state.selectedCategoryId)

        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(shopping.id))
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertTrue(effects.none { it is AddEditTransactionEffect.CategoryLearned })
        assertEquals(listOf(CategoryMemory("dmart", TransactionType.EXPENSE, shopping.id)), memory.memory.value)
    }

    @Test
    fun keepingTheSuggestion_orCorrectingNothing_remembersNothing() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        filledDmart(viewModel)

        // Changed, then changed back
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(shopping.id))
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(groceries.id))
        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertTrue(memory.memory.value.isEmpty())
        assertEquals(1, effects.count { it is AddEditTransactionEffect.CategoryLearned })
        assertTrue(AddEditTransactionEffect.Saved in effects)
    }

    @Test
    fun aMemoryThatCannotBeWritten_stillSavesTheTransaction() = runTest {
        memory.failWith = DomainError.StorageFailure()
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        filledDmart(viewModel)
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(shopping.id))

        viewModel.onEvent(AddEditTransactionEvent.SaveClicked)

        assertTrue(AddEditTransactionEffect.Saved in effects)
        assertEquals(1, transactions.current.size)
    }

    @Test
    fun aScannedBill_fromAMerchantFiledBefore_goesWhereItWasFiled() = runTest {
        memory.memory.value = listOf(CategoryMemory("cafe coffee day", TransactionType.EXPENSE, bills.id))
        billReader.result = Outcome.Success(recognizedText("[h=44] CAFE COFFEE DAY", "Grand Total ₹1,428.00"))
        val viewModel = viewModel()

        viewModel.onEvent(AddEditTransactionEvent.BillScanned("file:///cache/scan/bill.jpg"))

        assertEquals(bills.id, viewModel.state.selectedCategoryId)
        assertEquals(DraftSource.Bill, viewModel.state.draftFill?.source)
    }

    private companion object {
        const val TYPING_PAUSE = 300L
    }
}
