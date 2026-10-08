package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType

data class AddEditTransactionUiState(
    val title: String = "",
    val amount: String = "",
    val note: String = "", // Formerly 'title'
    val selectedCategory: Category? = null, // Now an Object, not a String
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val dateMillis: Long = System.currentTimeMillis(),
    val isDatePickerVisible: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isEditMode: Boolean = false,
    // Categories matching the current transactionType
    val availableCategories: List<Category> = emptyList(),
    val isCategorySheetVisible: Boolean = false
)
