package com.example.expensetrackerpro.presentation.manageCategories.uistate

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorEvent

sealed interface ManageCategoryEvent {
    data object SearchOpened : ManageCategoryEvent
    data object SearchClosed : ManageCategoryEvent
    data class SearchQueryChanged(val query: String) : ManageCategoryEvent
    data class TypeChanged(val type: TransactionType) : ManageCategoryEvent
    data object AddClicked : ManageCategoryEvent
    data class CategoryClicked(val category: Category) : ManageCategoryEvent

    /** Anything done in the add/edit sheet while it is open. */
    data class Editor(val event: CategoryEditorEvent) : ManageCategoryEvent

    // The delete dialog, opened from the sheet's delete button
    data class DeleteTargetSelected(val categoryId: CategoryId) : ManageCategoryEvent
    data object DeleteConfirmed : ManageCategoryEvent
    data object DeleteDismissed : ManageCategoryEvent
    data object MessageShown : ManageCategoryEvent
}
