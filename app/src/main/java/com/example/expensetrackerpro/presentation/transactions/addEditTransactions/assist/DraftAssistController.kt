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
import com.example.expensetrackerpro.domain.models.QuickAddBatch
import com.example.expensetrackerpro.domain.models.QuickAddPart
import com.example.expensetrackerpro.domain.models.QuickAddResult
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionDraft
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.sum
import com.example.expensetrackerpro.domain.usecase.categorymemory.ObserveCategoryMemoryUseCase
import com.example.expensetrackerpro.domain.usecase.categorymemory.RememberCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.GetFrequentTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.IsVoiceInputAvailableUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.MarkQuickAddUsedUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.ObserveQuickAddUsedUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.ParseQuickAddUseCase
import com.example.expensetrackerpro.domain.usecase.quickadd.SplitQuickAddUseCase
import com.example.expensetrackerpro.domain.usecase.scan.IsBillScannerAvailableUseCase
import com.example.expensetrackerpro.domain.usecase.scan.ScanBillUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ResolveTransactionDateUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.SaveTransactionsUseCase
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.draft.TransactionDraftReducer
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEffect
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEntryMode
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionUiState
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftField
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftSource
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddBatchPreview
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddBatchRow
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddEvent
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddPreview
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddUiState
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.ReviewPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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

    /** Puts the form back as a new transaction starts, after one of a batch was saved. Keeps its type. */
    fun resetForm()

    suspend fun send(effect: AddEditTransactionEffect)
}

/**
 * Everything that fills the transaction form for the user: a scanned bill, the quick-add bar
 * (typed or spoken), and the categories learnt from the user's corrections. One belongs to each
 * [com.example.expensetrackerpro.presentation.transactions.addEditTransactions.AddEditTransactionViewModel],
 * which forwards the events to it. Every source becomes a [TransactionDraft] that goes through
 * the same [TransactionDraftReducer], so nothing the user typed is overwritten and nothing is saved
 * until they tap Save. The user's frequent transactions are another source of drafts.
 *
 * A sentence with several transactions in it is shown as a batch: "Save all" stores every one of
 * them at once, the user having seen each value, or "Review one by one" fills the form with each
 * in turn, the next one coming in as the one before is saved.
 *
 * Of a scanned bill only the photo's URI is kept, in the saved state, until it has been read.
 */
class DraftAssistController @Inject constructor(
    private val scanBill: ScanBillUseCase,
    private val isBillScannerAvailable: IsBillScannerAvailableUseCase,
    private val parseQuickAdd: ParseQuickAddUseCase,
    private val splitQuickAdd: SplitQuickAddUseCase,
    private val saveTransactions: SaveTransactionsUseCase,
    private val getFrequentTransactions: GetFrequentTransactionsUseCase,
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
    private val chosenRowCategoryIds = mutableMapOf<Int, CategoryId>()

    // The rest of a batch being reviewed one by one: each part's words and draft, in order
    private val reviewQueue = ArrayDeque<Pair<String, TransactionDraft>>()
    private var reviewTotal = 0

    private var frequentJob: Job? = null

    // A category the user picked over the one a draft suggested, remembered on Save unless undone
    private var correction: CategoryId? = null
    private var isCorrectionHintShown = false

    /**
     * Works for as long as [scope] lives, on a new transaction only: a stored one is never filled
     * from a bill or a sentence. A bill the process did not survive reading is read again.
     *
     * [entryMode] asks for the scanner, the speech recognizer or the bar as the form opens, once:
     * not again after the screen is recreated. What the phone can't do falls back to the bar.
     */
    @OptIn(FlowPreview::class)
    fun start(
        scope: CoroutineScope,
        savedStateHandle: SavedStateHandle,
        host: DraftAssistHost,
        isEditMode: Boolean,
        entryMode: AddEntryMode = AddEntryMode.MANUAL
    ) {
        this.scope = scope
        this.savedStateHandle = savedStateHandle
        this.host = host
        if (isEditMode) return

        val isScanAvailable = isBillScannerAvailable()
        val isVoiceAvailable = isVoiceInputAvailable()
        val launchRequest = launchRequestFor(entryMode, isScanAvailable, isVoiceAvailable)
            .takeUnless { savedStateHandle.get<Boolean>(KEY_LAUNCH_HANDLED) == true }
        host.updateForm {
            it.copy(
                isScanAvailable = isScanAvailable,
                quickAdd = it.quickAdd.copy(
                    isAvailable = true,
                    isVoiceAvailable = isVoiceAvailable,
                    voiceLanguageTag = voiceLanguageTag(),
                    launchRequest = launchRequest
                )
            )
        }
        loadFrequent()

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
            // A batch has no single transaction to fill the form with, so it is gone through in turn
            QuickAddEvent.FillClicked -> if (host.form.quickAdd.batch != null) reviewOneByOne() else fill(focusAmount = false)
            is QuickAddEvent.ChipClicked -> if (host.form.quickAdd.batch != null) {
                // Of a batch only the shared date is changed from here; each row's category has its own event
                if (event.field == QuickAddField.DATE) updateQuickAdd { it.copy(editing = QuickAddField.DATE, editingRow = null) }
            } else when (event.field) {
                // The amount is changed in the form's own field, which gets the keypad
                QuickAddField.AMOUNT -> fill(focusAmount = true)
                QuickAddField.CATEGORY, QuickAddField.DATE -> updateQuickAdd { it.copy(editing = event.field) }
                QuickAddField.TITLE, QuickAddField.NOTE, QuickAddField.TYPE -> Unit
            }
            QuickAddEvent.ChipEditDismissed -> updateQuickAdd { it.copy(editing = null, editingRow = null) }
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
            is QuickAddEvent.FrequentSelected -> fillFromFrequent(event.index)
            is QuickAddEvent.BatchCategoryClicked ->
                updateQuickAdd { it.copy(editing = QuickAddField.CATEGORY, editingRow = event.row) }
            is QuickAddEvent.BatchCategoryChosen -> {
                chosenRowCategoryIds[event.row] = event.categoryId
                closeEditorAndRefresh()
            }
            QuickAddEvent.SaveAllClicked -> saveAll()
            QuickAddEvent.ReviewOneByOneClicked -> reviewOneByOne()
            QuickAddEvent.LaunchHandled -> {
                savedStateHandle[KEY_LAUNCH_HANDLED] = true
                updateQuickAdd { it.copy(launchRequest = null) }
            }
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
        // What is left of a batch goes too: the user stopped going through it
        reviewQueue.clear()
        host.updateForm {
            draftReducer.clear(it, host.allCategories).copy(quickAdd = it.quickAdd.copy(reviewPosition = null))
        }
    }

    /**
     * The form's type was switched, which changes the categories a sentence can be filed under
     * and which transactions are frequent.
     */
    fun onTypeChanged() {
        if (!host.form.quickAdd.isAvailable) return
        refreshPreview()
        loadFrequent()
    }

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

    /**
     * The transaction was saved under [categoryId]: a correction of the suggested category is
     * remembered. While a batch is reviewed one by one the form then holds the next of it, and
     * this returns true: the form is not done yet.
     */
    suspend fun onSaved(title: String, type: TransactionType, categoryId: CategoryId): Boolean {
        if (correction == categoryId) {
            when (rememberCategory(title, type, categoryId)) {
                is Outcome.Success -> correction = null
                // The transaction is saved. Remembering is only a help, so quick add simply guesses next time
                is Outcome.Failure -> Unit
            }
        }
        if (reviewQueue.isEmpty()) return false

        val saved = reviewTotal - reviewQueue.size
        host.resetForm()
        fillNextInReview()
        host.send(AddEditTransactionEffect.SavedOneOfMany(saved, reviewTotal))
        return true
    }

    private fun onTextChanged(value: String) {
        text.value = value
        if (value.isBlank()) clearChoices()
        updateQuickAdd {
            it.copy(
                text = value,
                preview = if (value.isBlank()) null else it.preview,
                batch = if (value.isBlank()) null else it.batch,
                editing = null,
                editingRow = null
            )
        }
    }

    private fun clearChoices() {
        chosenCategoryId = null
        chosenDateUtcMillis = null
        chosenRowCategoryIds.clear()
    }

    private fun closeEditorAndRefresh() {
        updateQuickAdd { it.copy(editing = null, editingRow = null) }
        refreshPreview()
    }

    /**
     * Reads the bar's text again, with the form's type, the user's categories and what was learnt:
     * as several transactions when it holds them, else as one.
     */
    private fun refreshPreview() {
        val form = host.form
        if (!form.quickAdd.isAvailable) return

        val batch = parseBatch(form)?.let { batchPreview(it, form) }
        if (batch != null) {
            updateQuickAdd { it.copy(batch = batch, preview = null) }
            return
        }
        val result = parse(form)
        updateQuickAdd { it.copy(batch = null, preview = result?.let { parsed -> preview(parsed, host.form) }) }
    }

    private fun parseBatch(form: AddEditTransactionUiState): QuickAddBatch? {
        val sentence = text.value.trim()
        if (sentence.isEmpty()) return null

        val batch = splitQuickAdd(sentence, form.transactionType, host.allCategories, learned) ?: return null
        return QuickAddBatch(
            batch.parts.mapIndexed { row, part ->
                part.copy(result = part.result.withChoices(form, chosenRowCategoryIds[row]))
            }
        )
    }

    private fun parse(form: AddEditTransactionUiState): QuickAddResult? {
        val sentence = text.value.trim()
        if (sentence.isEmpty()) return null

        return parseQuickAdd(sentence, form.transactionType, host.allCategories, learned).withChoices(form)
    }

    /** The result with what the user chose on the chips, which they are sure of. */
    private fun QuickAddResult.withChoices(
        form: AddEditTransactionUiState,
        categoryId: CategoryId? = chosenCategoryId
    ): QuickAddResult {
        var changed = draft
        val sure = confidence.toMutableMap()
        val type = draft.type ?: form.transactionType

        categoryId
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

    private fun batchPreview(batch: QuickAddBatch, form: AddEditTransactionUiState): QuickAddBatchPreview? {
        val today = resolveTransactionDate(DateSelection.Today)
        val rows = batch.parts.map { part ->
            val result = part.result
            val draft = result.draft
            val type = draft.type ?: form.transactionType
            val options = host.allCategories.filter { it.type == type }
            QuickAddBatchRow(
                amount = draft.amount ?: return null,
                amountConfidence = result.confidenceOf(QuickAddField.AMOUNT) ?: FieldConfidence.MISSING,
                category = options.find { it.id == draft.categoryId },
                categoryConfidence = result.confidenceOf(QuickAddField.CATEGORY) ?: FieldConfidence.MISSING,
                categoryOptions = options,
                title = draft.title,
                titleConfidence = result.confidenceOf(QuickAddField.TITLE) ?: FieldConfidence.MISSING,
                date = draft.dateMillis?.let { resolveTransactionDate(DateSelection.Picked(it)) },
                note = draft.note,
                type = type
            )
        }
        val dates = rows.map { it.date?.millis }.distinct()
        val types = rows.map { it.type }.distinct()
        val sharedDate = rows.first().date.takeIf { dates.size == 1 }

        return QuickAddBatchPreview(
            rows = rows,
            sharedDate = sharedDate,
            hasMixedDates = dates.size > 1,
            total = rows.map { it.amount }.sum().takeIf { types.size == 1 },
            type = types.singleOrNull(),
            canSaveAll = rows.all { it.title != null && it.category != null },
            pickerUtcMillis = sharedDate?.pickerUtcMillis ?: today.pickerUtcMillis,
            maxPickerUtcMillis = today.maxPickerUtcMillis
        )
    }

    /**
     * Stores every transaction of the batch, or none. The user has seen each value on its row, so
     * this is a deliberate save; the form is done once it succeeds.
     */
    private fun saveAll() {
        val form = host.form
        if (form.quickAdd.isSavingAll || form.quickAdd.batch?.canSaveAll != true) return
        val batch = parseBatch(form) ?: return
        val transactions = batch.parts.map { part -> transactionOf(part, form) ?: return }

        updateQuickAdd { it.copy(isSavingAll = true) }
        scope.launch {
            when (val result = saveTransactions(transactions)) {
                is Outcome.Success -> {
                    markQuickAddUsed()
                    host.send(AddEditTransactionEffect.Saved)
                }
                is Outcome.Failure -> {
                    // Nothing was stored, and the batch is still there to try again
                    updateQuickAdd { it.copy(isSavingAll = false) }
                    host.send(AddEditTransactionEffect.ShowError(result.error.toMessageRes()))
                }
            }
        }
    }

    /** The transaction [part] says, or null while it lacks a title or a category of its type. */
    private fun transactionOf(part: QuickAddPart, form: AddEditTransactionUiState): Transaction? {
        val draft = part.result.draft
        val type = draft.type ?: form.transactionType
        val category = host.allCategories.find { it.id == draft.categoryId && it.type == type } ?: return null
        val date = resolveTransactionDate(draft.dateMillis?.let { DateSelection.Picked(it) } ?: DateSelection.Today)

        return Transaction(
            title = draft.title ?: return null,
            amount = draft.amount ?: return null,
            note = draft.note.orEmpty(),
            dateMillis = date.millis,
            categoryId = category.id,
            categoryName = category.name,
            transactionType = type,
            categoryIcon = category.icon
        )
    }

    /** Fills the form with the first of the batch and keeps the rest for after each Save. */
    private fun reviewOneByOne() {
        val form = host.form
        val batch = parseBatch(form) ?: return

        reviewQueue.clear()
        // Each keeps the type it was read with, as the form's may change in between
        batch.parts.forEach { part ->
            reviewQueue.addLast(part.text to part.result.draft.copy(type = part.result.draft.type ?: form.transactionType))
        }
        reviewTotal = batch.parts.size
        text.value = ""
        clearChoices()
        fillNextInReview()
    }

    private fun fillNextInReview() {
        val (words, draft) = reviewQueue.removeFirstOrNull() ?: return
        val position = ReviewPosition(number = reviewTotal - reviewQueue.size, total = reviewTotal)
        correction = null

        host.updateForm {
            draftReducer.apply(it, draft, DraftSource.Text(words), host.allCategories)
                .copy(
                    quickAdd = it.quickAdd.copy(
                        text = "",
                        preview = null,
                        batch = null,
                        editing = null,
                        editingRow = null,
                        reviewPosition = position
                    )
                )
        }
        scope.launch { markQuickAddUsed() }
    }

    /** Fills the form with one of the user's frequent transactions, dated today. */
    private fun fillFromFrequent(index: Int) {
        val frequent = host.form.quickAdd.frequent.getOrNull(index) ?: return
        val draft = TransactionDraft(
            amount = frequent.usualAmount,
            title = frequent.title,
            type = frequent.type,
            categoryId = frequent.categoryId
        )
        correction = null

        host.updateForm { draftReducer.apply(it, draft, DraftSource.History(frequent.title), host.allCategories) }
        scope.launch { markQuickAddUsed() }
    }

    /** The frequent transactions of the form's type, read again when the type changes. */
    private fun loadFrequent() {
        val type = host.form.transactionType
        frequentJob?.cancel()
        frequentJob = scope.launch {
            val frequent = getFrequentTransactions(type)
            updateQuickAdd { it.copy(frequent = frequent) }
        }
    }

    private fun launchRequestFor(mode: AddEntryMode, isScanAvailable: Boolean, isVoiceAvailable: Boolean): AddEntryMode? =
        when (mode) {
            AddEntryMode.MANUAL -> null
            AddEntryMode.QUICK_ADD -> AddEntryMode.QUICK_ADD
            AddEntryMode.SCAN -> if (isScanAvailable) AddEntryMode.SCAN else AddEntryMode.QUICK_ADD
            AddEntryMode.VOICE -> if (isVoiceAvailable) AddEntryMode.VOICE else AddEntryMode.QUICK_ADD
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
        clearChoices()
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
        const val KEY_LAUNCH_HANDLED = "entry_mode_handled"
        const val TYPING_PAUSE_MILLIS = 300L
        const val ENGLISH_INDIA = "en-IN"
    }
}
