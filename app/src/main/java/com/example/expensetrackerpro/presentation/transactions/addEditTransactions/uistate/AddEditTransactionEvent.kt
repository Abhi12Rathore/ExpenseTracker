package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.TransactionType

sealed interface AddEditTransactionEvent {
    data class TypeChanged(val type: TransactionType) : AddEditTransactionEvent
    data class AmountChanged(val amount: String) : AddEditTransactionEvent
    data class TitleChanged(val title: String) : AddEditTransactionEvent
    data class NoteChanged(val note: String) : AddEditTransactionEvent
    data object NoteExpanded : AddEditTransactionEvent
    data object NoteRemoved : AddEditTransactionEvent
    data class CategorySelected(val categoryId: Int) : AddEditTransactionEvent

    /** The Today or Yesterday shortcut. */
    data class DateChoiceSelected(val choice: DateChoice) : AddEditTransactionEvent

    /** A date picker result, reported as midnight UTC of the chosen date. */
    data class DatePicked(val utcDayMillis: Long) : AddEditTransactionEvent
    data object CategorySheetOpened : AddEditTransactionEvent
    data object CategorySheetDismissed : AddEditTransactionEvent
    data class CategoryAdded(
        val name: String,
        val icon: CategoryIcon,
        val type: TransactionType
    ) : AddEditTransactionEvent
    data object SaveClicked : AddEditTransactionEvent
    data object DeleteClicked : AddEditTransactionEvent
    data object DeleteConfirmed : AddEditTransactionEvent
    data object DeleteDismissed : AddEditTransactionEvent
}
