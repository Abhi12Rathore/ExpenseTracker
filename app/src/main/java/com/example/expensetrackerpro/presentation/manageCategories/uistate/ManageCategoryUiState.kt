package com.example.expensetrackerpro.presentation.manageCategories.uistate

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategorySections
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorState

data class ManageCategoryUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    // The categories of selectedType that match searchQuery, with the count of each type
    val sections: CategorySections = CategorySections(),
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    // Non-null while the add/edit sheet is open
    val categoryEditor: CategoryEditorState? = null,
    // Non-null while a delete is waiting to be confirmed
    val pendingDelete: CategoryDeleteRequest? = null,
    // What went wrong last, until the screen has shown it
    val message: ManageCategoryMessage? = null
)

/** A category the user asked to delete, and where its transactions could go. */
data class CategoryDeleteRequest(
    val category: Category,
    // Every transaction filed under it, which must be moved before it can go
    val transactionCount: Int = 0,
    // The other categories of its type
    val targets: List<Category> = emptyList(),
    val selectedTargetId: Int? = null
) {
    val needsReassign: Boolean get() = transactionCount > 0

    val selectedTarget: Category? get() = targets.find { it.id == selectedTargetId }

    val isConfirmEnabled: Boolean get() = !needsReassign || selectedTarget != null
}

enum class ManageCategoryMessage { DELETE_FAILED }
