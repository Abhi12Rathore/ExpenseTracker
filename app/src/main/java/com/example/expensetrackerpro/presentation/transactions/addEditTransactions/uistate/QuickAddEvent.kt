package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.QuickAddField

/** Anything done in the quick-add bar or its preview. */
sealed interface QuickAddEvent {
    data class TextChanged(val text: String) : QuickAddEvent

    /** One of the example sentences was tapped: it goes into the bar. */
    data class ExampleSelected(val text: String) : QuickAddEvent

    /** The speech recognizer heard [text]. */
    data class VoiceRecognized(val text: String) : QuickAddEvent

    /** The speech recognizer could not be opened after all, so the microphone goes. */
    data object VoiceUnavailable : QuickAddEvent

    /** "Fill the form", the bar's send button or the keyboard's Done. */
    data object FillClicked : QuickAddEvent

    data class ChipClicked(val field: QuickAddField) : QuickAddEvent

    /** The category or date chip's short list was closed without a choice. */
    data object ChipEditDismissed : QuickAddEvent

    data class CategoryChosen(val categoryId: CategoryId) : QuickAddEvent

    /** Today or Yesterday in the date chip's list. */
    data class DateChoiceChosen(val choice: DateChoice) : QuickAddEvent

    /** A date picker result, as midnight UTC of the chosen day. */
    data class DatePicked(val utcDayMillis: Long) : QuickAddEvent

    /** Undo on "Next time … goes to …": the correction is not remembered. */
    data object LearnUndone : QuickAddEvent

    /** One of the frequent transactions was tapped: the form is filled with it. */
    data class FrequentSelected(val index: Int) : QuickAddEvent

    /** The category chip of the batch's [row] was tapped. */
    data class BatchCategoryClicked(val row: Int) : QuickAddEvent

    data class BatchCategoryChosen(val row: Int, val categoryId: CategoryId) : QuickAddEvent

    /** "Save all": every transaction of the batch is stored, or none. */
    data object SaveAllClicked : QuickAddEvent

    /** "Review one by one": the form is filled with each of the batch in turn. */
    data object ReviewOneByOneClicked : QuickAddEvent

    /** The screen opened what [QuickAddUiState.launchRequest] asked for. */
    data object LaunchHandled : QuickAddEvent
}
