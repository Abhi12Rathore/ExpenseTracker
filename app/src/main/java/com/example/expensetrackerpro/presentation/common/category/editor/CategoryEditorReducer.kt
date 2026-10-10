package com.example.expensetrackerpro.presentation.common.category.editor

import com.example.expensetrackerpro.domain.models.Category
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

    private companion object {
        const val NAME_MAX_LENGTH = ValidateCategoryNameUseCase.NAME_MAX_LENGTH
    }
}
