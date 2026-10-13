package com.example.expensetrackerpro.presentation.draft

import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.CategoryHint
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.DateSelection
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionDraft
import com.example.expensetrackerpro.domain.usecase.scan.ResolveCategoryHintUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.FormatAmountInputUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ResolveTransactionDateUseCase
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.utcDay
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.draft.TransactionDraftReducer
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionUiState
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftField
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftFill
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.withDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TransactionDraftReducerTest {

    // Thursday 8 Oct 2026, 12:00 in Kolkata
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)
    private val resolveDate = ResolveTransactionDateUseCase(FakeTimeProvider(now))
    private val reducer = TransactionDraftReducer(FormatAmountInputUseCase(), resolveDate, ResolveCategoryHintUseCase())

    private val groceries = category(id = 1, name = "Groceries").copy(builtInKey = BuiltInCategory.GROCERIES)
    private val health = category(id = 2, name = "Health").copy(builtInKey = BuiltInCategory.HEALTH)

    private val emptyForm = AddEditTransactionUiState(availableCategories = listOf(groceries, health))
        .withDate(resolveDate(DateSelection.Today))

    private val grocer = TransactionDraft(
        amount = Money(1152_50),
        otherAmounts = listOf(rupees(640)),
        title = "Reliance Fresh",
        dateMillis = utcDay(2026, Calendar.OCTOBER, 3),
        note = "Bill no. 88",
        categoryHint = CategoryHint.GROCERIES
    )

    @Test
    fun aDraft_fillsOnlyWhatItHas() {
        val form = reducer.apply(emptyForm, TransactionDraft(amount = rupees(250), title = "Chai Point"))

        assertEquals("250", form.amount)
        assertEquals("Chai Point", form.title)
        assertEquals("", form.note)
        assertEquals(DateChoice.TODAY, form.dateChoice)
        assertNull(form.selectedCategoryId)
        assertEquals(
            DraftFill(filledFields = setOf(DraftField.AMOUNT, DraftField.TITLE), amountOptions = listOf(rupees(250))),
            form.draftFill
        )
    }

    @Test
    fun paise_areKept_andALongNoteIsCutAtTheLimit() {
        val form = reducer.apply(emptyForm, grocer.copy(note = "n".repeat(250)))

        assertEquals("1152.5", form.amount)
        assertEquals(200, form.note.length)
        assertTrue(form.isNoteExpanded)
        assertEquals(millisAt(2026, Calendar.OCTOBER, 3, hour = 12), form.dateMillis)
        assertEquals(groceries.id, form.selectedCategoryId)
    }

    @Test
    fun aHintWithoutItsCategory_picksNone() {
        val form = reducer.apply(emptyForm, grocer.copy(categoryHint = CategoryHint.FUEL))

        assertNull(form.selectedCategoryId)
        assertEquals(DraftField.entries.toSet() - DraftField.CATEGORY, form.draftFill?.filledFields)
    }

    @Test
    fun aSecondDraft_replacesWhatTheFirstFilledIn_butNotWhatTheUserChangedSince() {
        val first = reducer.apply(emptyForm, grocer)
        val edited = reducer.fieldEdited(first.copy(title = "Weekly shop"), DraftField.TITLE)

        val second = reducer.apply(
            edited,
            TransactionDraft(amount = rupees(213), title = "Apollo Pharmacy", categoryHint = CategoryHint.HEALTH)
        )

        assertEquals("213", second.amount)
        assertEquals("Weekly shop", second.title)
        assertEquals(health.id, second.selectedCategoryId)
        // The first bill's note and date are gone, not left beside the second bill's amount
        assertEquals("", second.note)
        assertEquals(now, second.dateMillis)
        assertEquals(setOf(DraftField.AMOUNT, DraftField.CATEGORY), second.draftFill?.filledFields)
        assertEquals(listOf(rupees(213)), second.draftFill?.amountOptions)
    }

    @Test
    fun withoutADraftInTheForm_editsPicksAndClear_changeNothing() {
        assertSame(emptyForm, reducer.fieldEdited(emptyForm, DraftField.TITLE))
        assertSame(emptyForm, reducer.selectAmount(emptyForm, rupees(100)))
        assertSame(emptyForm, reducer.clear(emptyForm))
    }
}
