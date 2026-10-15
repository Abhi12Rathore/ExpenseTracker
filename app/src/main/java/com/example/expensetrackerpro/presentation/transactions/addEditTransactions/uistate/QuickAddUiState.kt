package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.TransactionDate
import com.example.expensetrackerpro.domain.models.TransactionType

/** The quick-add bar above the form: what was typed, and what was understood from it. */
data class QuickAddUiState(
    /** Offered on a new transaction only. */
    val isAvailable: Boolean = false,
    val text: String = "",
    /** What the text says, once it has been read; null while there is nothing to read. */
    val preview: QuickAddPreview? = null,
    /** Example sentences under the bar, until quick add has been used once. */
    val showExamples: Boolean = false,
    val isVoiceAvailable: Boolean = false,
    /** The language the system's speech recognizer listens for, as a BCP 47 tag: `en-IN`, `hi-IN`. */
    val voiceLanguageTag: String = "",
    /** The chip whose short list is open: the category or the date. */
    val editing: QuickAddField? = null
) {
    /** Whether the bar's text would fill the form now. */
    val canFill: Boolean get() = preview != null
}

/** The values understood from the text, each with how sure quick add is of it. */
data class QuickAddPreview(
    val amount: Money?,
    val amountConfidence: FieldConfidence,
    val category: Category?,
    val categoryConfidence: FieldConfidence,
    /** The categories of [type], for the category chip's short list. */
    val categoryOptions: List<Category>,
    val title: String?,
    val titleConfidence: FieldConfidence,
    /** Null when no date was said: the form keeps today. */
    val date: TransactionDate?,
    val note: String?,
    val type: TransactionType,
    /** Whether a word in the text said income or expense, so the form's type is changed. */
    val isTypeSaid: Boolean,
    /** The date picker's bounds, for "Pick a date" in the date chip's list. */
    val pickerUtcMillis: Long,
    val maxPickerUtcMillis: Long
)
