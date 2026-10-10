package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorState

data class AddEditTransactionUiState(
    val title: String = "",
    // As typed: plain digits with an optional decimal point, no grouping
    val amount: String = "",
    // The amount as money, or null while it is not a valid amount
    val validAmount: Money? = null,
    val note: String = "",
    val isNoteExpanded: Boolean = false,
    val selectedCategoryId: Int? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val dateMillis: Long = 0L,
    val dateChoice: DateChoice = DateChoice.TODAY,
    // The date and the latest selectable day as a date picker expects them: midnight UTC
    val pickerUtcMillis: Long = 0L,
    val maxPickerUtcMillis: Long = Long.MAX_VALUE,
    // Shown once the title has been edited and left blank
    val isTitleErrorVisible: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    // Non-null while the delete confirmation dialog is showing
    val transactionToDelete: Transaction? = null,
    // Categories matching the current transactionType
    val availableCategories: List<Category> = emptyList(),
    // Non-null while the new-category sheet is open
    val categoryEditor: CategoryEditorState? = null
) {
    val selectedCategory: Category?
        get() = availableCategories.find { it.id == selectedCategoryId }
}
