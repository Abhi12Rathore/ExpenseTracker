package com.example.expensetrackerpro.domain.models


import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType

data class AddTransactionUiState(
    val title:String="",
    val amount: String = "",
    val note: String = "", // Formerly 'title'
    val selectedCategory: Category? = null, // Now an Object, not a String
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val dateMillis: Long = System.currentTimeMillis(),
    val isDatePickerVisible: Boolean = false,
    val isSaveEnabled: Boolean = false
)
