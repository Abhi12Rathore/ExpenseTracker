package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.BuiltInCategory
import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.AddCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesByTypeUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.category.SaveCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.UpdateCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import com.example.expensetrackerpro.domain.usecase.scan.IsBillScannerAvailableUseCase
import com.example.expensetrackerpro.domain.usecase.scan.ParseBillUseCase
import com.example.expensetrackerpro.domain.usecase.scan.ResolveCategoryHintUseCase
import com.example.expensetrackerpro.domain.usecase.scan.ScanBillUseCase
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
import com.example.expensetrackerpro.fakes.FakeBillScannerAvailability
import com.example.expensetrackerpro.fakes.FakeBillTextReader
import com.example.expensetrackerpro.fakes.draftAssistController
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.recognizedText
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorReducer
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.AddEditTransactionViewModel
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.draft.TransactionDraftReducer
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEffect
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEvent
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftField
import com.example.expensetrackerpro.util.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
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

/** Scanning a bill into the Add transaction form. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionBillScanTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Thursday 8 Oct 2026, 12:00 in Kolkata
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)
    private val timeProvider = FakeTimeProvider(now)

    private val dining = category(id = 1, name = "Food & Dining", type = TransactionType.EXPENSE)
        .copy(builtInKey = BuiltInCategory.FOOD_DINING)
    private val bills = category(id = 2, name = "Bills", type = TransactionType.EXPENSE)
    private val salary = category(id = 3, name = "Salary", type = TransactionType.INCOME)

    private val categories = FakeCategoryRepository(listOf(dining, bills, salary))
    private val transactions = FakeTransactionRepository()
    private val billReader = FakeBillTextReader()
    private val scannerAvailability = FakeBillScannerAvailability()

    // Everything a bill can fill: it never says whether it is income or expense
    private val billFields = DraftField.entries.toSet() - DraftField.TYPE

    private val photo = "file:///cache/scan/bill.jpg"

    // Everything on it can be read: total, merchant, date (three days ago) and bill number
    private val cafeBill = recognizedText(
        "[h=44] CAFE COFFEE DAY",
        "Bill No: 4521 Dt: 05/10/2026",
        "Sandwich 1 290.00",
        "Sub Total 1360.00",
        "Grand Total ₹1,428.00"
    )

    // Torn off above the total
    private val tornBill = recognizedText("[h=44] CAFE COFFEE DAY", "Sandwich 1 290.00", "Sub Total 1360.00")

    private fun viewModel(
        transactionId: Int? = null,
        savedState: Map<String, Any?> = emptyMap()
    ) = AddEditTransactionViewModel(
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
        assist = draftAssistController(timeProvider, billReader, scannerAvailability),
        savedStateHandle = SavedStateHandle(
            savedState + if (transactionId != null) mapOf("transactionId" to transactionId) else emptyMap()
        )
    )

    private fun TestScope.effectsOf(viewModel: AddEditTransactionViewModel): List<AddEditTransactionEffect> {
        val effects = mutableListOf<AddEditTransactionEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.toList(effects)
        }
        return effects
    }

    /** A new transaction whose form [bill] has just been scanned into. */
    private fun scanned(bill: com.example.expensetrackerpro.domain.models.RecognizedText = cafeBill): AddEditTransactionViewModel {
        billReader.result = Outcome.Success(bill)
        return viewModel().also { it.onEvent(AddEditTransactionEvent.BillScanned(photo)) }
    }

    // --- Entry point

    @Test
    fun scanning_isOfferedForANewTransaction_whereTheScannerIsThere() = runTest {
        assertTrue(viewModel().uiState.value.isScanAvailable)

        scannerAvailability.available = false
        assertFalse(viewModel().uiState.value.isScanAvailable)
    }

    @Test
    fun scanning_isNotOfferedWhileEditing_andAPhotoIsIgnoredThere() = runTest {
        transactions.saveTransaction(transaction(id = 5, amount = rupees(42), title = "Lunch"))
        billReader.result = Outcome.Success(cafeBill)
        val viewModel = viewModel(transactionId = 5)

        viewModel.onEvent(AddEditTransactionEvent.BillScanned(photo))

        assertFalse(viewModel.uiState.value.isScanAvailable)
        assertTrue(billReader.read.isEmpty())
        assertEquals("Lunch", viewModel.uiState.value.title)
        assertNull(viewModel.uiState.value.draftFill)
    }

    @Test
    fun aScannerThatCannotBeShown_isNoLongerOffered() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(AddEditTransactionEvent.BillScannerFailed)

        assertFalse(viewModel.uiState.value.isScanAvailable)
    }

    // --- Filling the form

    @Test
    fun aScannedBill_fillsTheEmptyForm_andMarksEveryFieldItFilled() = runTest {
        val state = scanned().uiState.value

        assertEquals(listOf(DocumentUri(photo)), billReader.read)
        assertEquals("1428", state.amount)
        assertEquals(rupees(1428), state.validAmount)
        assertEquals("Cafe Coffee Day", state.title)
        assertEquals("Bill no. 4521", state.note)
        assertTrue(state.isNoteExpanded)
        assertEquals(dining.id, state.selectedCategoryId)
        // The bill's day, at the current time of day like any picked date
        assertEquals(millisAt(2026, Calendar.OCTOBER, 5, hour = 12), state.dateMillis)
        assertEquals(DateChoice.OTHER, state.dateChoice)

        assertEquals(billFields, state.draftFill?.filledFields)
        assertEquals(listOf(rupees(1428), rupees(1360), rupees(290)), state.draftFill?.amountOptions)
        assertFalse(state.draftFill?.isAmountMissing ?: true)
        assertFalse(state.isReadingBill)
        // Filled, but saved only when the user says so
        assertTrue(state.isSaveEnabled)
        assertTrue(transactions.current.isEmpty())
    }

    @Test
    fun aScannedBill_neverReplacesWhatTheUserEntered() = runTest {
        billReader.result = Outcome.Success(cafeBill)
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("99"))
        viewModel.onEvent(AddEditTransactionEvent.TitleChanged("Coffee with Asha"))
        viewModel.onEvent(AddEditTransactionEvent.NoteExpanded)
        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("Her treat next time"))
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(bills.id))
        viewModel.onEvent(AddEditTransactionEvent.DateChoiceSelected(DateChoice.YESTERDAY))

        viewModel.onEvent(AddEditTransactionEvent.BillScanned(photo))

        val state = viewModel.uiState.value
        assertEquals("99", state.amount)
        assertEquals("Coffee with Asha", state.title)
        assertEquals("Her treat next time", state.note)
        assertEquals(bills.id, state.selectedCategoryId)
        assertEquals(DateChoice.YESTERDAY, state.dateChoice)
        assertEquals(emptySet<DraftField>(), state.draftFill?.filledFields)
        // The bill's amounts are still there to pick from
        assertEquals(listOf(rupees(1428), rupees(1360), rupees(290)), state.draftFill?.amountOptions)
        assertFalse(state.draftFill?.isAmountMissing ?: true)
    }

    @Test
    fun aDateLeftAtToday_isFilled_butOneChosenAsToday_isKept() = runTest {
        billReader.result = Outcome.Success(cafeBill)
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.DateChoiceSelected(DateChoice.TODAY))

        viewModel.onEvent(AddEditTransactionEvent.BillScanned(photo))

        assertEquals(DateChoice.TODAY, viewModel.uiState.value.dateChoice)
        assertFalse(viewModel.uiState.value.isFromDraft(DraftField.DATE))
    }

    @Test
    fun aSuggestedCategory_needsThatCategoryForTheCurrentType() = runTest {
        billReader.result = Outcome.Success(cafeBill)
        val viewModel = viewModel()
        viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.INCOME))

        viewModel.onEvent(AddEditTransactionEvent.BillScanned(photo))

        assertNull(viewModel.uiState.value.selectedCategoryId)
        assertFalse(viewModel.uiState.value.isFromDraft(DraftField.CATEGORY))
        assertTrue(viewModel.uiState.value.isFromDraft(DraftField.TITLE))
    }

    // --- Tags

    @Test
    fun editingAFilledField_removesItsMark_andLeavesTheOthers() = runTest {
        val viewModel = scanned()
        fun filled() = viewModel.uiState.value.draftFill?.filledFields

        viewModel.onEvent(AddEditTransactionEvent.TitleChanged("Cafe Coffee Day, MG Road"))
        assertEquals(billFields - DraftField.TITLE, filled())

        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("1430"))
        viewModel.onEvent(AddEditTransactionEvent.NoteChanged("Bill no. 4521, with Asha"))
        assertEquals(setOf(DraftField.DATE, DraftField.CATEGORY), filled())

        viewModel.onEvent(AddEditTransactionEvent.DatePicked(millisAt(2026, Calendar.OCTOBER, 6)))
        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(bills.id))
        assertEquals(emptySet<DraftField>(), filled())
        // The banner, and with it "Clear", stays
        assertEquals(listOf(rupees(1428), rupees(1360), rupees(290)), viewModel.uiState.value.draftFill?.amountOptions)
    }

    @Test
    fun confirmingAFilledValueByChoosingItAgain_alsoRemovesItsMark() = runTest {
        val viewModel = scanned()

        viewModel.onEvent(AddEditTransactionEvent.CategorySelected(dining.id))
        viewModel.onEvent(AddEditTransactionEvent.NoteRemoved)

        assertEquals(
            setOf(DraftField.AMOUNT, DraftField.TITLE, DraftField.DATE),
            viewModel.uiState.value.draftFill?.filledFields
        )
        assertEquals(dining.id, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun anAmountEditThatIsTurnedDown_keepsTheMark() = runTest {
        val viewModel = scanned()

        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("14x28"))

        assertEquals("1428", viewModel.uiState.value.amount)
        assertTrue(viewModel.uiState.value.isFromDraft(DraftField.AMOUNT))
    }

    @Test
    fun changingTheType_takesTheSuggestedCategoryAndItsMarkAway() = runTest {
        val viewModel = scanned()

        viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.INCOME))

        assertNull(viewModel.uiState.value.selectedCategoryId)
        assertEquals(
            billFields - DraftField.CATEGORY,
            viewModel.uiState.value.draftFill?.filledFields
        )
    }

    // --- Clear

    @Test
    fun clear_removesWhatTheBillFilledIn() = runTest {
        val viewModel = scanned()

        viewModel.onEvent(AddEditTransactionEvent.BillFillCleared)

        val state = viewModel.uiState.value
        assertEquals("", state.amount)
        assertEquals("", state.title)
        assertEquals("", state.note)
        assertFalse(state.isNoteExpanded)
        assertNull(state.selectedCategoryId)
        assertEquals(DateChoice.TODAY, state.dateChoice)
        assertEquals(now, state.dateMillis)
        assertNull(state.draftFill)
        assertFalse(state.isSaveEnabled)
        // And the card is back, for another scan
        assertTrue(state.isScanAvailable)
    }

    @Test
    fun clear_keepsWhatTheUserChangedSince() = runTest {
        val viewModel = scanned()
        viewModel.onEvent(AddEditTransactionEvent.TitleChanged("Team coffee"))
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("1500"))

        viewModel.onEvent(AddEditTransactionEvent.BillFillCleared)

        val state = viewModel.uiState.value
        assertEquals("Team coffee", state.title)
        assertEquals("1500", state.amount)
        assertEquals("", state.note)
        assertNull(state.selectedCategoryId)
        assertNull(state.draftFill)
    }

    // --- Other amounts

    @Test
    fun pickingAnotherAmount_putsItInTheForm() = runTest {
        val viewModel = scanned()

        viewModel.onEvent(AddEditTransactionEvent.BillAmountSelected(rupees(1360)))

        assertEquals("1360", viewModel.uiState.value.amount)
        assertEquals(rupees(1360), viewModel.uiState.value.validAmount)
        assertTrue(viewModel.uiState.value.isFromDraft(DraftField.AMOUNT))
    }

    @Test
    fun pickingAnAmount_replacesATypedOne_andMarksItAsTheBills() = runTest {
        val viewModel = scanned()
        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("15"))

        viewModel.onEvent(AddEditTransactionEvent.BillAmountSelected(rupees(290)))

        assertEquals("290", viewModel.uiState.value.amount)
        assertTrue(viewModel.uiState.value.isFromDraft(DraftField.AMOUNT))
    }

    @Test
    fun anAmountTheBillDidNotOffer_orWithoutABill_changesNothing() = runTest {
        val viewModel = scanned()
        viewModel.onEvent(AddEditTransactionEvent.BillAmountSelected(rupees(7)))
        assertEquals("1428", viewModel.uiState.value.amount)

        val untouched = viewModel()
        untouched.onEvent(AddEditTransactionEvent.BillAmountSelected(rupees(1428)))
        assertEquals("", untouched.uiState.value.amount)
    }

    // --- When the scan doesn't fully work

    @Test
    fun aBillWithoutATotal_fillsTheRest_andAsksForTheAmount() = runTest {
        val viewModel = scanned(tornBill)

        val state = viewModel.uiState.value
        assertEquals("", state.amount)
        assertEquals("Cafe Coffee Day", state.title)
        assertEquals(setOf(DraftField.TITLE, DraftField.CATEGORY), state.draftFill?.filledFields)
        assertEquals(listOf(rupees(1360), rupees(290)), state.draftFill?.amountOptions)
        assertTrue(state.draftFill?.isAmountMissing ?: false)
        assertFalse(state.isSaveEnabled)

        viewModel.onEvent(AddEditTransactionEvent.BillAmountSelected(rupees(1360)))

        assertEquals("1360", viewModel.uiState.value.amount)
        assertFalse(viewModel.uiState.value.draftFill?.isAmountMissing ?: true)
        assertTrue(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun typingTheMissingAmount_alsoAnswersTheRequestForIt() = runTest {
        val viewModel = scanned(tornBill)

        viewModel.onEvent(AddEditTransactionEvent.AmountChanged("1428"))

        assertFalse(viewModel.uiState.value.draftFill?.isAmountMissing ?: true)
        assertFalse(viewModel.uiState.value.isFromDraft(DraftField.AMOUNT))
    }

    @Test
    fun anUnreadableBill_leavesTheFormAsItWas_andOffersAnotherTry() = runTest {
        billReader.result = Outcome.Failure(DomainError.BillUnreadable)
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        viewModel.onEvent(AddEditTransactionEvent.TitleChanged("Lunch"))
        val before = viewModel.uiState.value

        viewModel.onEvent(AddEditTransactionEvent.BillScanned(photo))

        assertEquals(before, viewModel.uiState.value)
        assertEquals(
            listOf<AddEditTransactionEffect>(
                AddEditTransactionEffect.BillUnreadable(R.string.error_bill_unreadable)
            ),
            effects
        )
    }

    @Test
    fun aPhotoThatIsNoBill_isTreatedAsUnreadable() = runTest {
        billReader.result = Outcome.Success(recognizedText("[h=40] HAPPY BIRTHDAY", "With love"))
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.onEvent(AddEditTransactionEvent.BillScanned(photo))

        assertEquals("", viewModel.uiState.value.title)
        assertNull(viewModel.uiState.value.draftFill)
        assertEquals(1, effects.size)
    }

    // --- Reading, and surviving the scanner

    @Test
    fun whileTheBillIsRead_theFormSaysSo_andASecondPhotoWaitsItsTurn() = runTest {
        val gate = CompletableDeferred<Unit>()
        billReader.gate = gate
        billReader.result = Outcome.Success(cafeBill)
        val viewModel = viewModel()

        viewModel.onEvent(AddEditTransactionEvent.BillScanned(photo))
        viewModel.onEvent(AddEditTransactionEvent.BillScanned("file:///cache/scan/other.jpg"))

        assertTrue(viewModel.uiState.value.isReadingBill)
        assertEquals("", viewModel.uiState.value.title)
        assertEquals(listOf(DocumentUri(photo)), billReader.read)

        gate.complete(Unit)

        assertFalse(viewModel.uiState.value.isReadingBill)
        assertEquals("Cafe Coffee Day", viewModel.uiState.value.title)
    }

    @Test
    fun aPhotoStillInTheSavedState_isReadWhenTheScreenComesBack() = runTest {
        billReader.result = Outcome.Success(cafeBill)

        // The process was killed while the scanner was open: the result arrives, then the
        // process dies again before the bill has been read
        val viewModel = viewModel(savedState = mapOf("bill_image_uri" to photo))

        assertEquals(listOf(DocumentUri(photo)), billReader.read)
        assertEquals("1428", viewModel.uiState.value.amount)
        assertEquals("Cafe Coffee Day", viewModel.uiState.value.title)
        assertEquals(dining.id, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun aSavedPhoto_isNotReadWhileEditing() = runTest {
        transactions.saveTransaction(transaction(id = 5, amount = rupees(42), title = "Lunch"))

        viewModel(transactionId = 5, savedState = mapOf("bill_image_uri" to photo))

        assertTrue(billReader.read.isEmpty())
    }
}
