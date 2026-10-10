package com.example.expensetrackerpro.presentation.common.category.editor

import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryIconGroup
import com.example.expensetrackerpro.domain.models.TransactionType

sealed interface CategoryEditorEvent {
    data class NameChanged(val name: String) : CategoryEditorEvent
    data class TypeChanged(val type: TransactionType) : CategoryEditorEvent
    data class IconGroupSelected(val group: CategoryIconGroup) : CategoryEditorEvent
    data class IconSelected(val icon: CategoryIcon) : CategoryEditorEvent

    // What comes next is up to the screen showing the sheet
    data object SaveClicked : CategoryEditorEvent
    data object DeleteClicked : CategoryEditorEvent
    data object Dismissed : CategoryEditorEvent
}
