package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.draft

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.DateSelection
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionDraft
import com.example.expensetrackerpro.domain.usecase.scan.ResolveCategoryHintUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.FormatAmountInputUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ResolveTransactionDateUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateTransactionDraftUseCase
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionUiState
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftField
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftFill
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftSource
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.withDate
import javax.inject.Inject

/**
 * Puts a [TransactionDraft] into the transaction form, whatever the draft came from, and keeps
 * track of which fields still hold its values. A draft never replaces what the user entered.
 */
class TransactionDraftReducer @Inject constructor(
    private val formatAmountInput: FormatAmountInputUseCase,
    private val resolveTransactionDate: ResolveTransactionDateUseCase,
    private val resolveCategoryHint: ResolveCategoryHintUseCase
) {

    /**
     * Fills the fields [draft] has a value for and the user has not set. What an earlier draft
     * filled in goes first, so two drafts are never mixed.
     *
     * The draft's type is taken only while no category is picked, as a picked one belongs to the
     * form's type. [allCategories], of both types, gives the new type's categories straight away.
     */
    fun apply(
        state: AddEditTransactionUiState,
        draft: TransactionDraft,
        source: DraftSource = DraftSource.Bill,
        allCategories: List<Category> = emptyList()
    ): AddEditTransactionUiState {
        var form = clear(state, allCategories)
        val filled = mutableSetOf<DraftField>()

        val typeBefore = form.transactionType
        val type = draft.type
        if (type != null && type != form.transactionType && form.selectedCategory == null) {
            form = form.copy(
                transactionType = type,
                availableCategories = allCategories.filter { it.type == type }
            )
            filled += DraftField.TYPE
        }

        val amount = draft.amount
        if (amount != null && form.amount.isEmpty()) {
            form = form.copy(amount = formatAmountInput(amount))
            filled += DraftField.AMOUNT
        }

        val title = draft.title
        if (title != null && form.title.isBlank()) {
            form = form.copy(title = title, isTitleErrorVisible = false)
            filled += DraftField.TITLE
        }

        val dateMillis = draft.dateMillis
        if (dateMillis != null && !form.isDateEdited) {
            form = form.withDate(resolveTransactionDate(DateSelection.Picked(dateMillis)))
            filled += DraftField.DATE
        }

        val note = draft.note
        if (note != null && form.note.isBlank()) {
            form = form.copy(note = note.take(NOTE_MAX_LENGTH), isNoteExpanded = true)
            filled += DraftField.NOTE
        }

        // Only a suggestion: a category that is not there for this type is simply not picked
        val category = if (form.selectedCategory != null) {
            null
        } else {
            draft.categoryId?.let { id -> form.availableCategories.find { it.id == id } }
                ?: draft.categoryHint?.let { resolveCategoryHint(it, form.availableCategories) }
        }
        if (category != null) {
            form = form.copy(selectedCategoryId = category.id)
            filled += DraftField.CATEGORY
        }

        return form.copy(
            draftFill = DraftFill(
                filledFields = filled,
                amountOptions = listOfNotNull(amount) + draft.otherAmounts,
                isAmountMissing = form.amount.isEmpty(),
                source = source,
                typeBeforeFill = typeBefore.takeIf { DraftField.TYPE in filled },
                suggestedCategoryId = category?.id
            )
        )
    }

    /** The user changed [field], so it is theirs now and no longer shown as filled in. */
    fun fieldEdited(state: AddEditTransactionUiState, field: DraftField): AddEditTransactionUiState {
        val fill = state.draftFill ?: return state

        return state.copy(
            draftFill = fill.copy(
                filledFields = fill.filledFields - field,
                isAmountMissing = fill.isAmountMissing && field != DraftField.AMOUNT
            )
        )
    }

    /** Takes one of the draft's other amounts instead. An amount it never offered changes nothing. */
    fun selectAmount(state: AddEditTransactionUiState, amount: Money): AddEditTransactionUiState {
        val fill = state.draftFill ?: return state
        if (amount !in fill.amountOptions) return state

        return state.copy(
            amount = formatAmountInput(amount),
            draftFill = fill.copy(filledFields = fill.filledFields + DraftField.AMOUNT, isAmountMissing = false)
        )
    }

    /**
     * Empties the fields that still hold the draft's values. What the user changed stays, and so
     * does a type the draft set once the user has picked a category of it.
     */
    fun clear(state: AddEditTransactionUiState, allCategories: List<Category> = emptyList()): AddEditTransactionUiState {
        val fill = state.draftFill ?: return state
        val filled = fill.filledFields
        var form = state.copy(draftFill = null)

        if (DraftField.AMOUNT in filled) form = form.copy(amount = "")
        if (DraftField.TITLE in filled) form = form.copy(title = "", isTitleErrorVisible = false)
        if (DraftField.DATE in filled) form = form.withDate(resolveTransactionDate(DateSelection.Today))
        if (DraftField.NOTE in filled) form = form.copy(note = "", isNoteExpanded = false)
        if (DraftField.CATEGORY in filled) form = form.copy(selectedCategoryId = null)

        val typeBefore = fill.typeBeforeFill
        if (DraftField.TYPE in filled && typeBefore != null && form.selectedCategoryId == null) {
            form = form.copy(
                transactionType = typeBefore,
                availableCategories = allCategories.filter { it.type == typeBefore }
            )
        }

        return form
    }

    private companion object {
        const val NOTE_MAX_LENGTH = ValidateTransactionDraftUseCase.NOTE_MAX_LENGTH
    }
}
