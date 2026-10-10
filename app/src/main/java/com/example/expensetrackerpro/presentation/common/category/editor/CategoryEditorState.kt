package com.example.expensetrackerpro.presentation.common.category.editor

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryDraft
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryIconGroup
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.TransactionType

/** The add/edit category sheet's form. A screen showing the sheet keeps one of these while it is open. */
data class CategoryEditorState(
    // The stored category being edited; null while creating a new one
    val editing: Category? = null,
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val icon: CategoryIcon = CategoryIcon.DEFAULT,
    // The tab of the icon picker that is showing
    val iconGroup: CategoryIconGroup = icon.group,
    val nameValidation: CategoryNameValidation = CategoryNameValidation.BLANK,
    // A blank name is only called out once the field has been typed in
    val isNameEdited: Boolean = false,
    val isSaving: Boolean = false
) {
    val isEditMode: Boolean get() = editing != null

    /** What to show under the name field, or null while there is nothing to point out. */
    val nameError: CategoryNameValidation?
        get() = when (nameValidation) {
            CategoryNameValidation.VALID -> null
            CategoryNameValidation.BLANK -> nameValidation.takeIf { isNameEdited }
            CategoryNameValidation.TOO_LONG,
            CategoryNameValidation.DUPLICATE -> nameValidation
        }

    val isSaveEnabled: Boolean get() = nameValidation == CategoryNameValidation.VALID && !isSaving

    fun toDraft() = CategoryDraft(id = editing?.id, name = name, type = type, icon = icon)
}
