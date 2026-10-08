package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType

sealed interface AddEditTransactionEvent {
    data class TypeChanged(val type: TransactionType) : AddEditTransactionEvent
    data class AmountChanged(val amount: String) : AddEditTransactionEvent
    data class NoteChanged(val note: String) : AddEditTransactionEvent
    data class CategorySelected(val category: Category) : AddEditTransactionEvent
    data class DateSelected(val millis: Long?) : AddEditTransactionEvent
    data class DatePickerToggled(val show: Boolean) : AddEditTransactionEvent
    data object CategorySheetOpened : AddEditTransactionEvent
    data object CategorySheetDismissed : AddEditTransactionEvent
    data class CategoryAdded(
        val name: String,
        val iconName: String,
        val type: TransactionType
    ) : AddEditTransactionEvent
    data object SaveClicked : AddEditTransactionEvent
}
