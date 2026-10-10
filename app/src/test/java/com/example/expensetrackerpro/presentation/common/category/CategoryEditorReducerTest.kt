package com.example.expensetrackerpro.presentation.common.category

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.InvalidInputReason
import com.example.expensetrackerpro.domain.models.CategoryDraft
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryIconGroup
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorEvent
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorReducer
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryEditorReducerTest {

    private val reducer = CategoryEditorReducer(ValidateCategoryNameUseCase())

    private val food = category(id = 1, name = "Food", icon = CategoryIcon.RESTAURANT)
    private val salary = category(id = 2, name = "Salary", type = TransactionType.INCOME, icon = CategoryIcon.PAYMENTS)
    private val existing = listOf(food, salary)

    private fun CategoryEditorState.on(event: CategoryEditorEvent) = reducer.reduce(this, event, existing)

    private fun newExpense() = reducer.open(TransactionType.EXPENSE, existing = existing)

    // --- A save that failed

    @Test
    fun aNameTheSaveTurnedDown_isCalledOut_andSavingCanBeTriedAgain() {
        val saving = newExpense().on(CategoryEditorEvent.NameChanged("Pets")).copy(isSaving = true)

        val cases = mapOf(
            DomainError.DuplicateName to CategoryNameValidation.DUPLICATE,
            DomainError.InvalidInput(InvalidInputReason.NAME_BLANK) to CategoryNameValidation.BLANK,
            DomainError.InvalidInput(InvalidInputReason.NAME_TOO_LONG) to CategoryNameValidation.TOO_LONG
        )

        cases.forEach { (error, expected) ->
            val state = reducer.saveFailed(saving, error)

            assertEquals(expected, state.nameError)
            assertFalse(state.isSaving)
            assertFalse(state.isSaveEnabled)
        }
    }

    @Test
    fun aSaveThatFailedForAnotherReason_leavesTheNameAlone_andSavingCanBeTriedAgain() {
        val saving = newExpense().on(CategoryEditorEvent.NameChanged("Pets")).copy(isSaving = true)

        val errors = listOf(
            DomainError.StorageFailure("disk I/O error"),
            DomainError.NotFound,
            DomainError.CategoryInUse,
            DomainError.InvalidInput(InvalidInputReason.INCOMPATIBLE_REASSIGN_TARGET)
        )

        errors.forEach { error ->
            val state = reducer.saveFailed(saving, error)

            assertNull(state.nameError)
            assertFalse(state.isSaving)
            assertTrue(state.isSaveEnabled)
        }
    }

    // --- Opening

    @Test
    fun openedForANewCategory_isEmptyOfTheGivenType_withNothingToComplainAboutYet() {
        val state = reducer.open(TransactionType.INCOME, existing = existing)

        assertFalse(state.isEditMode)
        assertEquals("", state.name)
        assertEquals(TransactionType.INCOME, state.type)
        assertEquals(CategoryIcon.DEFAULT, state.icon)
        assertEquals(CategoryIcon.DEFAULT.group, state.iconGroup)
        // Blank, but the field has not been touched
        assertNull(state.nameError)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun openedOnACategory_isFilledFromIt_onItsIconsTab_andReadyToSave() {
        val state = reducer.open(TransactionType.EXPENSE, category = salary, existing = existing)

        assertTrue(state.isEditMode)
        assertEquals("Salary", state.name)
        // The category's own type wins over the tab the sheet was opened from
        assertEquals(TransactionType.INCOME, state.type)
        assertEquals(CategoryIcon.PAYMENTS, state.icon)
        assertEquals(CategoryIconGroup.MONEY, state.iconGroup)
        assertNull(state.nameError)
        assertTrue(state.isSaveEnabled)
    }

    // --- Name

    @Test
    fun aGoodName_enablesSave() {
        val state = newExpense().on(CategoryEditorEvent.NameChanged("Gym"))

        assertEquals("Gym", state.name)
        assertNull(state.nameError)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun aNameClearedAgain_isCalledOutAsBlank() {
        val state = newExpense()
            .on(CategoryEditorEvent.NameChanged("G"))
            .on(CategoryEditorEvent.NameChanged(""))

        assertEquals(CategoryNameValidation.BLANK, state.nameError)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun aNameAlreadyUsedByTheType_isADuplicate() {
        val state = newExpense().on(CategoryEditorEvent.NameChanged("food"))

        assertEquals(CategoryNameValidation.DUPLICATE, state.nameError)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun switchingType_reChecksTheName() {
        val duplicate = newExpense().on(CategoryEditorEvent.NameChanged("Food"))

        val asIncome = duplicate.on(CategoryEditorEvent.TypeChanged(TransactionType.INCOME))
        assertEquals(TransactionType.INCOME, asIncome.type)
        assertNull(asIncome.nameError)
        assertTrue(asIncome.isSaveEnabled)

        val backToExpense = asIncome.on(CategoryEditorEvent.TypeChanged(TransactionType.EXPENSE))
        assertEquals(CategoryNameValidation.DUPLICATE, backToExpense.nameError)
    }

    @Test
    fun typedPastTheLimit_isCutAtThirtyCharacters() {
        val state = newExpense().on(CategoryEditorEvent.NameChanged("a".repeat(45)))

        assertEquals("a".repeat(30), state.name)
        assertNull(state.nameError)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun whileEditing_theOwnNameIsFine_anothersIsNot_andTheTypeStays() {
        val editing = reducer.open(TransactionType.EXPENSE, category = food, existing = existing)

        assertNull(editing.on(CategoryEditorEvent.NameChanged("FOOD")).nameError)

        val bills = category(id = 3, name = "Bills")
        val taken = reducer.reduce(editing, CategoryEditorEvent.NameChanged("Bills"), existing + bills)
        assertEquals(CategoryNameValidation.DUPLICATE, taken.nameError)

        assertEquals(
            TransactionType.EXPENSE,
            editing.on(CategoryEditorEvent.TypeChanged(TransactionType.INCOME)).type
        )
    }

    @Test
    fun aNameTakenWhileTheSheetIsOpen_isCaughtWhenRevalidated() {
        val state = newExpense().on(CategoryEditorEvent.NameChanged("Gym"))

        val revalidated = reducer.validated(state, existing + category(id = 9, name = "gym"))

        assertEquals(CategoryNameValidation.DUPLICATE, revalidated.nameError)
    }

    // --- Icons

    @Test
    fun switchingTab_keepsTheChosenIcon_andPickingOneKeepsTheTab() {
        val state = newExpense()
            .on(CategoryEditorEvent.IconSelected(CategoryIcon.PETS))
            .on(CategoryEditorEvent.IconGroupSelected(CategoryIconGroup.MONEY))

        assertEquals(CategoryIcon.PETS, state.icon)
        assertEquals(CategoryIconGroup.MONEY, state.iconGroup)

        val picked = state.on(CategoryEditorEvent.IconSelected(CategoryIcon.SAVINGS))
        assertEquals(CategoryIcon.SAVINGS, picked.icon)
        assertEquals(CategoryIconGroup.MONEY, picked.iconGroup)
    }

    // --- What the screen takes over

    @Test
    fun saveDeleteAndDismiss_leaveTheFormAlone() {
        val state = newExpense().on(CategoryEditorEvent.NameChanged("Gym"))

        assertEquals(state, state.on(CategoryEditorEvent.SaveClicked))
        assertEquals(state, state.on(CategoryEditorEvent.DeleteClicked))
        assertEquals(state, state.on(CategoryEditorEvent.Dismissed))
    }

    @Test
    fun whileSaving_saveIsDisabled() {
        val state = newExpense().on(CategoryEditorEvent.NameChanged("Gym"))

        assertFalse(state.copy(isSaving = true).isSaveEnabled)
    }

    @Test
    fun theDraft_carriesTheFormAndTheEditedId() {
        val created = newExpense()
            .on(CategoryEditorEvent.NameChanged("Gym"))
            .on(CategoryEditorEvent.IconSelected(CategoryIcon.FITNESS_CENTER))
        assertEquals(
            CategoryDraft(id = null, name = "Gym", type = TransactionType.EXPENSE, icon = CategoryIcon.FITNESS_CENTER),
            created.toDraft()
        )

        val edited = reducer.open(TransactionType.EXPENSE, category = food, existing = existing)
        assertEquals(food.id, edited.toDraft().id)
    }
}
