package com.example.expensetrackerpro.presentation.manageCategories.uistate

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType

sealed interface ManageCategoryEvent {
    data class SearchQueryChanged(val query: String) : ManageCategoryEvent
    data class TypeChanged(val type: TransactionType) : ManageCategoryEvent
    data object AddClicked : ManageCategoryEvent
    data class EditClicked(val category: Category) : ManageCategoryEvent
    data object SheetDismissed : ManageCategoryEvent
    data class CategorySaved(
        val name: String,
        val iconName: String,
        val type: TransactionType
    ) : ManageCategoryEvent
    data class DeleteClicked(val category: Category) : ManageCategoryEvent
}
