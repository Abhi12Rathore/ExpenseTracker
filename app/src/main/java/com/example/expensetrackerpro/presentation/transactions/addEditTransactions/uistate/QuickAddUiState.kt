package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.FrequentTransaction
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
    val editing: QuickAddField? = null,
    /** With [editing], the row of [batch] whose chip it is; null for the single preview and the shared date. */
    val editingRow: Int? = null,
    /** The user's most frequent transactions of the form's type, to repeat in one tap. */
    val frequent: List<FrequentTransaction> = emptyList(),
    /** What the text says when it holds several transactions; then [preview] is null. */
    val batch: QuickAddBatchPreview? = null,
    /** "Save all" is storing the batch. */
    val isSavingAll: Boolean = false,
    /** Where the form is in a batch being reviewed one by one, for the banner; null otherwise. */
    val reviewPosition: ReviewPosition? = null,
    /** What the screen should open as it starts: the scanner, the speech recognizer or the bar. Null once done. */
    val launchRequest: AddEntryMode? = null
) {
    /** Whether the bar's text would fill the form now. */
    val canFill: Boolean get() = preview != null || batch != null
}

/** The [number]th transaction of [total] being reviewed in the form. */
data class ReviewPosition(val number: Int, val total: Int)

/** Several transactions said in one sentence, each as its own row of chips. */
data class QuickAddBatchPreview(
    val rows: List<QuickAddBatchRow>,
    /** The date every row has, or null when they differ (each row then shows its own) or none was said. */
    val sharedDate: TransactionDate?,
    /** Whether the rows have different dates. */
    val hasMixedDates: Boolean,
    /** The amounts added up, or null when the rows mix income and expense. */
    val total: Money?,
    /** Every row is all expense, all income, or a mix: what the heading calls them. */
    val type: TransactionType?,
    /** Every row has an amount, a title and a category, so all of them can be saved at once. */
    val canSaveAll: Boolean,
    val pickerUtcMillis: Long,
    val maxPickerUtcMillis: Long
)

/** One transaction of a [QuickAddBatchPreview]. */
data class QuickAddBatchRow(
    val amount: Money,
    val amountConfidence: FieldConfidence,
    val category: Category?,
    val categoryConfidence: FieldConfidence,
    val categoryOptions: List<Category>,
    val title: String?,
    val titleConfidence: FieldConfidence,
    /** Null when no date was said for any row: today. */
    val date: TransactionDate?,
    val note: String?,
    val type: TransactionType
)

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
