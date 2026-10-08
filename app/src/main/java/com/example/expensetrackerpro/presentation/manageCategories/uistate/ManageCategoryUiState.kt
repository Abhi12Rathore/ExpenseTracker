package com.example.expensetrackerpro.presentation.manageCategories.uistate

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType

data class ManageCategoryUiState(
    // Already filtered by selectedType and searchQuery
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    // Requirement: Expense visible by default
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val isSheetOpen: Boolean = false,
    // Null while the sheet is in "Add" mode
    val categoryToEdit: Category? = null,
    // Set when a delete was refused because the category still has transactions
    val deleteBlockedCategoryName: String? = null
)
