package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.assist

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.DateSelection
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.QuickAddResult
import com.example.expensetrackerpro.domain.models.TransactionDraft
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.categorymemory.ObserveCategoryMemoryUseCase
import com.example.expensetrackerpro.domain.usecase.categorymemory.RememberCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.IsVoiceInputAvailableUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.MarkQuickAddUsedUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.ObserveQuickAddUsedUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.ParseQuickAddUseCase
import com.example.expensetrackerpro.domain.usecase.scan.IsBillScannerAvailableUseCase
import com.example.expensetrackerpro.domain.usecase.scan.ScanBillUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ResolveTransactionDateUseCase
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.draft.TransactionDraftReducer
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEffect
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionUiState
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftField
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftSource
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddEvent
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddPreview
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * The form the Add/Edit transaction screen shows and changes, as the [DraftAssistController]
 * needs it. Implemented by the screen's ViewModel.
 */
interface DraftAssistHost {
    val form: AddEditTransactionUiState

    /** The user's categories of both types. */
    val allCategories: List<Category>

    fun updateForm(change: (AddEditTransactionUiState) -> AddEditTransactionUiState)

    suspend fun send(effect: AddEditTransactionEffect)
}

/**
 * Everything that fills the transaction form for the user: a scanned bill, the quick-add bar
 * (typed or spoken), and the categories learnt from the user's corrections. One belongs to each
 * [com.example.expensetrackerpro.presentation.transactions.addEditTransactions.AddEditTransactionViewModel],
 * which forwards the events to it. Every source becomes a [TransactionDraft] that goes through
 * the same [TransactionDraftReducer], so nothing the user typed is overwritten and nothing is saved
 * until they tap Save.
 *
 * Of a scanned bill only the photo's URI is kept, in the saved state, until it has been read.
 */
class DraftAssistController @Inject constructor(
    private val scanBill: ScanBillUseCase,
    private val isBillScannerAvailable: IsBillScannerAvailableUseCase,
    private val parseQuickAdd: ParseQuickAddUseCase,
    private val observeCategoryMemory: ObserveCategoryMemoryUseCase,
    private val rememberCategory: RememberCategoryUseCase,
    private val observeQuickAddUsed: ObserveQuickAddUsedUseCase,
    private val markQuickAddUsed: MarkQuickAddUsedUseCase,
    private val isVoiceInputAvailable: IsVoiceInputAvailableUseCase,
    private val appLocaleProvider: AppLocaleProvider,
    private val resolveTransactionDate: ResolveTransactionDateUseCase,
    private val draftReducer: TransactionDraftReducer
) {
    private lateinit var scope: CoroutineScope
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var host: DraftAssistHost

    // The text as typed, read once typing pauses
    private val text = MutableStateFlow("")

    private var learned: List<CategoryMemory> = emptyList()

    // What the user changed on the preview's chips, kept while the text is edited
    private var chosenCategoryId: CategoryId? = null
    private var chosenDateUtcMillis: Long? = null

    // A category the user picked over the one a draft suggested, remembered on Save unless undone
    private var correction: CategoryId? = null
    private var isCorrectionHintShown = false

    /**
     * Works for as long as [scope] lives, on a new transaction only: a stored one is never filled
     * from a bill or a sentence. A bill the process did not survive reading is read again.
     */
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope, savedStateHandle: SavedStateHandle, host: DraftAssistHost, isEditMode: Boolean) {
        this.scope = scope
        this.savedStateHandle = savedStateHandle
        this.host = host
        if (isEditMode) return

        host.updateForm {
            it.copy(
                isScanAvailable = isBillScannerAvailable(),
                quickAdd = it.quickAdd.copy(
                    isAvailable = true,
                    isVoiceAvailable = isVoiceInputAvailable(),
                    voiceLanguageTag = voiceLanguageTag()
                )
            )
        }

        scope.launch {
            observeQuickAddUsed().collect { used -> updateQuickAdd { it.copy(showExamples = !used) } }
        }
        scope.launch {
            observeCategoryMemory().collect { memory ->
                learned = memory
                refreshPreview()
            }
        }
        scope.launch {
            // The first value is the empty bar, which has nothing to read
            text.drop(1).debounce(TYPING_PAUSE_MILLIS).collect { refreshPreview() }
        }

        savedStateHandle.get<String>(KEY_BILL_IMAGE)?.let { readBill(DocumentUri(it)) }
    }

    fun onEvent(event: QuickAddEvent) {
        when (event) {
            is QuickAddEvent.TextChanged -> onTextChanged(event.text)
            // Chosen, not typed: read at once
            is QuickAddEvent.ExampleSelected -> {
                onTextChanged(event.text)
                refreshPreview()
            }
            is QuickAddEvent.VoiceRecognized -> {
                onTextChanged(event.text)
                refreshPreview()
            }
            QuickAddEvent.VoiceUnavailable -> updateQuickAdd { it.copy(isVoiceAvailable = false) }
            QuickAddEvent.FillClicked -> fill(focusAmount = false)
            is QuickAddEvent.ChipClicked -> when (event.field) {
                // The amount is changed in the form's own field, which gets the keypad
                QuickAddField.AMOUNT -> fill(focusAmount = true)
                QuickAddField.CATEGORY, QuickAddField.DATE -> updateQuickAdd { it.copy(editing = event.field) }
                QuickAddField.TITLE, QuickAddField.NOTE, QuickAddField.TYPE -> Unit
            }
            QuickAddEvent.ChipEditDismissed -> updateQuickAdd { it.copy(editing = null) }
            is QuickAddEvent.CategoryChosen -> {
                chosenCategoryId = event.categoryId
                closeEditorAndRefresh()
            }
            is QuickAddEvent.DateChoiceChosen -> {
                val selection = when (event.choice) {
                    DateChoice.TODAY -> DateSelection.Today
                    DateChoice.YESTERDAY -> DateSelection.Yesterday
                    // Any other date comes from the picker, as DatePicked
                    DateChoice.OTHER -> return
                }
                chosenDateUtcMillis = resolveTransactionDate(selection).pickerUtcMillis
                closeEditorAndRefresh()
            }
            is QuickAddEvent.DatePicked -> {
                chosenDateUtcMillis = event.utcDayMillis
                closeEditorAndRefresh()
            }
            QuickAddEvent.LearnUndone -> correction = null
        }
    }

    fun onBillScanned(image: DocumentUri) = readBill(image)

    fun onBillScannerFailed() = host.updateForm { it.copy(isScanAvailable = false) }

    // What the form's own events do to a draft's values, kept with the drafts

    fun fieldEdited(state: AddEditTransactionUiState, field: DraftField): AddEditTransactionUiState =
        draftReducer.fieldEdited(state, field)

    fun selectAmount(state: AddEditTransactionUiState, amount: Money): AddEditTransactionUiState =
        draftReducer.selectAmount(state, amount)

    fun clearFill() {
        correction = null
        host.updateForm { draftReducer.clear(it, host.allCategories) }
    }

    /** The form's type was switched, which changes the categories a sentence can be filed under. */
    fun onTypeChanged() = refreshPreview()

    /**
     * The user picked [categoryId] in the form. Over what a draft suggested, or where it found
     * none, it is a correction, remembered for the title on Save. The first time a title is
     * corrected the user is told, and can undo it.
     */
    fun onCategoryPicked(categoryId: CategoryId) {
        val form = host.form
        val fill = form.draftFill ?: return
        if (categoryId == fill.suggestedCategoryId) {
            correction = null
            return
        }

        correction = categoryId
        val title = form.title.trim()
        val category = form.availableCategories.find { it.id == categoryId } ?: return
        val key = CategoryMemory.keyOf(title)
        val isKnown = learned.any { it.titleKey == key && it.type == form.transactionType }
        if (key.isEmpty() || isKnown || isCorrectionHintShown) return

        isCorrectionHintShown = true
        scope.launch { host.send(AddEditTransactionEffect.CategoryLearned(title, category.name)) }
    }

    /** The transaction was saved under [categoryId]: a correction of the suggested category is remembered. */
    suspend fun onSaved(title: String, type: TransactionType, categoryId: CategoryId) {
        if (correction != categoryId) return

        when (rememberCategory(title, type, categoryId)) {
            is Outcome.Success -> correction = null
            // The transaction is saved. Remembering is only a help, so quick add simply guesses next time
            is Outcome.Failure -> Unit
        }
    }

    private fun onTextChanged(value: String) {
        text.value = value
        if (value.isBlank()) {
            chosenCategoryId = null
            chosenDateUtcMillis = null
        }
        updateQuickAdd {
            it.copy(text = value, preview = if (value.isBlank()) null else it.preview, editing = null)
        }
    }

    private fun closeEditorAndRefresh() {
        updateQuickAdd { it.copy(editing = null) }
        refreshPreview()
    }

    /** Reads the bar's text again, with the form's type, the user's categories and what was learnt. */
    private fun refreshPreview() {
        val form = host.form
        if (!form.quickAdd.isAvailable) return
        val result = parse(form)

        updateQuickAdd { it.copy(preview = result?.let { parsed -> preview(parsed, host.form) }) }
    }

    private fun parse(form: AddEditTransactionUiState): QuickAddResult? {
        val sentence = text.value.trim()
        if (sentence.isEmpty()) return null

        return parseQuickAdd(sentence, form.transactionType, host.allCategories, learned).withChoices(form)
    }

    /** The result with what the user chose on the chips, which they are sure of. */
    private fun QuickAddResult.withChoices(form: AddEditTransactionUiState): QuickAddResult {
        var changed = draft
        val sure = confidence.toMutableMap()
        val type = draft.type ?: form.transactionType

        chosenCategoryId
            ?.takeIf { id -> host.allCategories.any { it.id == id && it.type == type } }
            ?.let { id ->
                changed = changed.copy(categoryId = id)
                sure[QuickAddField.CATEGORY] = FieldConfidence.SURE
            }
        chosenDateUtcMillis?.let { day ->
            changed = changed.copy(dateMillis = day)
            sure[QuickAddField.DATE] = FieldConfidence.SURE
        }

        return QuickAddResult(changed, sure)
    }

    private fun preview(result: QuickAddResult, form: AddEditTransactionUiState): QuickAddPreview {
        val draft = result.draft
        val type = draft.type ?: form.transactionType
        val options = host.allCategories.filter { it.type == type }
        val date = draft.dateMillis?.let { resolveTransactionDate(DateSelection.Picked(it)) }
        val today = resolveTransactionDate(DateSelection.Today)

        return QuickAddPreview(
            amount = draft.amount,
            amountConfidence = result.confidenceOf(QuickAddField.AMOUNT) ?: FieldConfidence.MISSING,
            category = options.find { it.id == draft.categoryId },
            categoryConfidence = result.confidenceOf(QuickAddField.CATEGORY) ?: FieldConfidence.MISSING,
            categoryOptions = options,
            title = draft.title,
            titleConfidence = result.confidenceOf(QuickAddField.TITLE) ?: FieldConfidence.MISSING,
            date = date,
            note = draft.note,
            type = type,
            isTypeSaid = draft.type != null,
            pickerUtcMillis = date?.pickerUtcMillis ?: today.pickerUtcMillis,
            maxPickerUtcMillis = today.maxPickerUtcMillis
        )
    }

    /**
     * Puts what the bar's text says into the form, through the same reducer as a bill, and empties
     * the bar. With no amount in the text, or when the amount chip was tapped, the keypad comes up.
     */
    private fun fill(focusAmount: Boolean) {
        val form = host.form
        val sentence = text.value.trim()
        val result = parse(form) ?: return

        host.updateForm {
            draftReducer.apply(it, result.draft, DraftSource.Text(sentence), host.allCategories)
                .copy(quickAdd = it.quickAdd.copy(text = "", preview = null, editing = null))
        }
        text.value = ""
        chosenCategoryId = null
        chosenDateUtcMillis = null
        correction = null

        scope.launch {
            markQuickAddUsed()
            if (focusAmount || host.form.amount.isEmpty()) host.send(AddEditTransactionEffect.FocusAmount)
        }
    }

    /**
     * Reads the photo and fills the form from it. The photo's address is kept in the saved state
     * until it has been read, so a scan the process did not survive is picked up again.
     */
    private fun readBill(image: DocumentUri) {
        // A bill fills in a new transaction, never one that is already stored
        if (host.form.isEditMode || host.form.isReadingBill) return

        savedStateHandle[KEY_BILL_IMAGE] = image.uri
        host.updateForm { it.copy(isReadingBill = true) }
        scope.launch {
            val result = scanBill(image)
            savedStateHandle.remove<String>(KEY_BILL_IMAGE)

            when (result) {
                is Outcome.Success -> {
                    correction = null
                    host.updateForm {
                        val draft = result.value.withLearnedCategory(it.transactionType)
                        draftReducer.apply(it.copy(isReadingBill = false), draft, DraftSource.Bill, host.allCategories)
                    }
                }
                is Outcome.Failure -> {
                    // The form stays exactly as it was
                    host.updateForm { it.copy(isReadingBill = false) }
                    host.send(AddEditTransactionEffect.BillUnreadable(result.error.toMessageRes()))
                }
            }
        }
    }

    /** A bill from a merchant the user filed before goes where they filed it. */
    private fun TransactionDraft.withLearnedCategory(type: TransactionType): TransactionDraft {
        val key = title?.let(CategoryMemory::keyOf) ?: return this
        val remembered = learned.firstOrNull { it.titleKey == key && it.type == type } ?: return this

        return copy(categoryId = remembered.categoryId)
    }

    private fun updateQuickAdd(change: (QuickAddUiState) -> QuickAddUiState) {
        host.updateForm { it.copy(quickAdd = change(it.quickAdd)) }
    }

    /** The app's language, as India speaks English when the app is in English. */
    private fun voiceLanguageTag(): String {
        val locale = appLocaleProvider.current()
        return if (locale.language == Locale.ENGLISH.language) ENGLISH_INDIA else locale.toLanguageTag()
    }

    private companion object {
        const val KEY_BILL_IMAGE = "bill_image_uri"
        const val TYPING_PAUSE_MILLIS = 300L
        const val ENGLISH_INDIA = "en-IN"
    }
}
