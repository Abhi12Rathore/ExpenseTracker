package com.example.expensetrackerpro.presentation.common.category.editor

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.InvalidInputReason
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import javax.inject.Inject

/**
 * The editor's form logic, shared by every ViewModel whose screen shows the sheet. Each call
 * takes the stored categories, which the name is checked against.
 */
class CategoryEditorReducer @Inject constructor(
    private val validateCategoryName: ValidateCategoryNameUseCase
) {

    /** The form as the sheet opens: empty for a new category of [type], or filled from [category]. */
    fun open(
        type: TransactionType,
        category: Category? = null,
        existing: List<Category>
    ): CategoryEditorState {
        val state = if (category != null) {
            CategoryEditorState(
                editing = category,
                name = category.name,
                type = category.type,
                icon = category.icon
            )
        } else {
            CategoryEditorState(type = type)
        }

        return validated(state, existing)
    }

    /** Applies a change to the form. Save, delete and dismiss leave it as it is. */
    fun reduce(
        state: CategoryEditorState,
        event: CategoryEditorEvent,
        existing: List<Category>
    ): CategoryEditorState {
        val changed = when (event) {
            is CategoryEditorEvent.NameChanged ->
                // Longer text, such as a paste, is cut at the limit
                state.copy(name = event.name.take(NAME_MAX_LENGTH), isNameEdited = true)
            is CategoryEditorEvent.TypeChanged ->
                // A stored category keeps its type: its transactions were saved as that type
                if (state.isEditMode) state else state.copy(type = event.type)
            is CategoryEditorEvent.IconGroupSelected -> state.copy(iconGroup = event.group)
            is CategoryEditorEvent.IconSelected -> state.copy(icon = event.icon)
            CategoryEditorEvent.SaveClicked,
            CategoryEditorEvent.DeleteClicked,
            CategoryEditorEvent.Dismissed -> state
        }

        return validated(changed, existing)
    }

    /** Re-checks the name, for when the stored categories have changed under an open sheet. */
    fun validated(state: CategoryEditorState, existing: List<Category>): CategoryEditorState =
        state.copy(
            nameValidation = validateCategoryName(state.name, state.type, state.editing?.id, existing)
        )

    /** The form after a save failed with [error]: a name that was turned down is called out. */
    fun saveFailed(state: CategoryEditorState, error: DomainError): CategoryEditorState {
        val nameValidation = nameProblemOf(error) ?: return state.copy(isSaving = false)

        return state.copy(nameValidation = nameValidation, isNameEdited = true, isSaving = false)
    }

    /**
     * True when [error] is about the name, which the sheet shows under the name field. Any
     * other failure has to be reported by the screen showing the sheet.
     */
    fun isNameProblem(error: DomainError): Boolean = nameProblemOf(error) != null

    private fun nameProblemOf(error: DomainError): CategoryNameValidation? = when (error) {
        DomainError.DuplicateName -> CategoryNameValidation.DUPLICATE
        is DomainError.InvalidInput -> when (error.reason) {
            InvalidInputReason.NAME_BLANK -> CategoryNameValidation.BLANK
            InvalidInputReason.NAME_TOO_LONG -> CategoryNameValidation.TOO_LONG
            InvalidInputReason.INCOMPATIBLE_REASSIGN_TARGET -> null
        }
        DomainError.NotFound,
        DomainError.CategoryInUse,
        is DomainError.StorageFailure,
        DomainError.FileWriteFailed,
        DomainError.FileReadFailed,
        DomainError.BackupInvalid,
        DomainError.BackupTooNew -> null
    }

    private companion object {
        const val NAME_MAX_LENGTH = ValidateCategoryNameUseCase.NAME_MAX_LENGTH
    }
}
