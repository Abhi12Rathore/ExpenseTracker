package com.example.expensetrackerpro.presentation.transactions.addEditTransactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.onFailure
import com.example.expensetrackerpro.domain.error.onSuccess
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.DateSelection
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesByTypeUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.category.SaveCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.scan.IsBillScannerAvailableUseCase
import com.example.expensetrackerpro.domain.usecase.scan.ScanBillUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.FormatAmountInputUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionByIdUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ResolveTransactionDateUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.SanitizeAmountInputUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.SaveTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateTransactionDraftUseCase
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorEvent
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorReducer
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorState
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEffect
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEvent
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.draft.TransactionDraftReducer
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionUiState
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftField
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.withDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val getTransactionById: GetTransactionByIdUseCase,
    getCategoriesByType: GetCategoriesByTypeUseCase,
    private val saveTransaction: SaveTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val validateTransactionDraft: ValidateTransactionDraftUseCase,
    private val sanitizeAmountInput: SanitizeAmountInputUseCase,
    private val formatAmountInput: FormatAmountInputUseCase,
    private val resolveTransactionDate: ResolveTransactionDateUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val saveCategory: SaveCategoryUseCase,
    private val categoryEditorReducer: CategoryEditorReducer,
    private val scanBill: ScanBillUseCase,
    isBillScannerAvailable: IsBillScannerAvailableUseCase,
    private val draftReducer: TransactionDraftReducer,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Nav argument; -1 (the default) means "add" mode
    private val transactionId: TransactionId? =
        savedStateHandle.get<Int>("transactionId")?.takeIf { it != -1 }?.let(::TransactionId)

    // The stored transaction being edited, once it has loaded
    private var original: Transaction? = null

    // Of both types: what a new category's name is checked against
    private var allCategories: List<Category> = emptyList()

    private val _uiState = MutableStateFlow(
        AddEditTransactionUiState(
            isEditMode = transactionId != null,
            // A bill fills in a new transaction, never one that is already stored
            isScanAvailable = transactionId == null && isBillScannerAvailable()
        ).withDate(resolveTransactionDate(DateSelection.Today))
    )
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    // A Channel, not state: each effect is handled once, and waits if nobody is collecting yet
    private val _effects = Channel<AddEditTransactionEffect>(Channel.BUFFERED)
    val effects: Flow<AddEditTransactionEffect> = _effects.receiveAsFlow()

    init {
        observeCategories(getCategoriesByType)
        observeAllCategories()
        if (transactionId != null) {
            loadExistingTransaction(transactionId)
        }
        // The process was killed while the scanner was open or the bill was being read
        savedStateHandle.get<String>(KEY_BILL_IMAGE)?.let { readBill(DocumentUri(it)) }
    }

    fun onEvent(event: AddEditTransactionEvent) {
        when (event) {
            is AddEditTransactionEvent.TypeChanged -> onTypeChange(event.type)
            is AddEditTransactionEvent.AmountChanged -> updateForm {
                val amount = sanitizeAmountInput(it.amount, event.amount)
                // An edit that was turned down, such as a second decimal point, changed nothing
                if (amount == it.amount) it else it.copy(amount = amount).edited(DraftField.AMOUNT)
            }
            is AddEditTransactionEvent.TitleChanged -> updateForm {
                it.copy(title = event.title, isTitleErrorVisible = event.title.isBlank())
                    .edited(DraftField.TITLE)
            }
            is AddEditTransactionEvent.NoteChanged ->
                // Longer text, such as a paste, is cut at the limit
                updateForm { it.copy(note = event.note.take(NOTE_MAX_LENGTH)).edited(DraftField.NOTE) }
            AddEditTransactionEvent.NoteExpanded -> updateForm { it.copy(isNoteExpanded = true) }
            AddEditTransactionEvent.NoteRemoved ->
                updateForm { it.copy(note = "", isNoteExpanded = false).edited(DraftField.NOTE) }
            is AddEditTransactionEvent.CategorySelected ->
                updateForm { it.copy(selectedCategoryId = event.categoryId).edited(DraftField.CATEGORY) }
            is AddEditTransactionEvent.DateChoiceSelected -> onDateChoice(event.choice)
            is AddEditTransactionEvent.DatePicked -> setDate(DateSelection.Picked(event.utcDayMillis))
            AddEditTransactionEvent.CategorySheetOpened -> updateForm {
                it.copy(
                    categoryEditor = categoryEditorReducer.open(it.transactionType, existing = allCategories)
                )
            }
            is AddEditTransactionEvent.CategoryEditor -> onCategoryEditorEvent(event.event)
            AddEditTransactionEvent.SaveClicked -> save()
            AddEditTransactionEvent.DeleteClicked ->
                updateForm { it.copy(transactionToDelete = original) }
            AddEditTransactionEvent.DeleteDismissed ->
                updateForm { it.copy(transactionToDelete = null) }
            AddEditTransactionEvent.DeleteConfirmed -> delete()
            is AddEditTransactionEvent.BillScanned -> readBill(DocumentUri(event.uri))
            AddEditTransactionEvent.BillScannerFailed -> updateForm { it.copy(isScanAvailable = false) }
            is AddEditTransactionEvent.BillAmountSelected ->
                updateForm { draftReducer.selectAmount(it, event.amount) }
            AddEditTransactionEvent.BillFillCleared -> updateForm(draftReducer::clear)
        }
    }

    /** Whenever the type changes, a new query fetches the categories of that type. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCategories(getCategoriesByType: GetCategoriesByTypeUseCase) {
        viewModelScope.launch {
            _uiState
                .map { it.transactionType }
                .distinctUntilChanged()
                .flatMapLatest { type -> getCategoriesByType(type) }
                .collect { categories -> updateForm { it.copy(availableCategories = categories) } }
        }
    }

    private fun observeAllCategories() {
        viewModelScope.launch {
            getCategories().collect { categories ->
                allCategories = categories
                updateEditor { categoryEditorReducer.validated(it, categories) }
            }
        }
    }

    private fun loadExistingTransaction(id: TransactionId) {
        viewModelScope.launch {
            val transaction = getTransactionById(id) ?: return@launch
            original = transaction

            updateForm {
                it.copy(
                    title = transaction.title,
                    amount = formatAmountInput(transaction.amount),
                    note = transaction.note,
                    isNoteExpanded = transaction.note.isNotEmpty(),
                    transactionType = transaction.transactionType,
                    selectedCategoryId = transaction.categoryId
                ).withDate(resolveTransactionDate(DateSelection.Stored(transaction.dateMillis)))
            }
        }
    }

    private fun onTypeChange(newType: TransactionType) {
        updateForm {
            // An expense category can't stay on an income, and the other way round
            val categoryId = it.selectedCategoryId.takeIf { _ -> it.selectedCategory?.type == newType }
            val changed = it.copy(transactionType = newType, selectedCategoryId = categoryId)

            // A category that had to go is no longer what the bill suggested
            if (categoryId == it.selectedCategoryId) changed else changed.edited(DraftField.CATEGORY)
        }
    }

    private fun onDateChoice(choice: DateChoice) {
        val selection = when (choice) {
            DateChoice.TODAY -> DateSelection.Today
            DateChoice.YESTERDAY -> DateSelection.Yesterday
            // Any other date comes from the picker, as DatePicked
            DateChoice.OTHER -> return
        }
        setDate(selection)
    }

    private fun setDate(selection: DateSelection) {
        updateForm {
            it.withDate(resolveTransactionDate(selection))
                .copy(isDateEdited = true)
                .edited(DraftField.DATE)
        }
    }

    /**
     * Reads the photo and fills the form from it. The photo's address is kept in the saved state
     * until it has been read, so a scan the process did not survive is picked up again.
     */
    private fun readBill(image: DocumentUri) {
        if (transactionId != null || _uiState.value.isReadingBill) return

        savedStateHandle[KEY_BILL_IMAGE] = image.uri
        updateForm { it.copy(isReadingBill = true) }
        viewModelScope.launch {
            val result = scanBill(image)
            savedStateHandle.remove<String>(KEY_BILL_IMAGE)

            when (result) {
                is Outcome.Success ->
                    updateForm { draftReducer.apply(it.copy(isReadingBill = false), result.value) }
                is Outcome.Failure -> {
                    // The form stays exactly as it was
                    updateForm { it.copy(isReadingBill = false) }
                    _effects.send(AddEditTransactionEffect.BillUnreadable(result.error.toMessageRes()))
                }
            }
        }
    }

    private fun AddEditTransactionUiState.edited(field: DraftField): AddEditTransactionUiState =
        draftReducer.fieldEdited(this, field)

    private fun onCategoryEditorEvent(event: CategoryEditorEvent) {
        when (event) {
            CategoryEditorEvent.Dismissed -> updateForm { it.copy(categoryEditor = null) }
            CategoryEditorEvent.SaveClicked -> saveNewCategory()
            // The sheet only creates categories here, so there is nothing stored to delete
            CategoryEditorEvent.DeleteClicked -> Unit
            else -> updateEditor { categoryEditorReducer.reduce(it, event, allCategories) }
        }
    }

    private fun saveNewCategory() {
        val editor = _uiState.value.categoryEditor ?: return
        if (!editor.isSaveEnabled) return

        updateEditor { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = saveCategory(editor.toDraft())) {
                is Outcome.Success -> updateForm {
                    it.copy(
                        categoryEditor = null,
                        // A category of the other type can't be picked for this transaction
                        selectedCategoryId = if (it.transactionType == editor.type) {
                            result.value
                        } else {
                            it.selectedCategoryId
                        }
                    ).let { form ->
                        if (form.selectedCategoryId == it.selectedCategoryId) form else form.edited(DraftField.CATEGORY)
                    }
                }
                is Outcome.Failure -> when (result.error) {
                    DomainError.NotFound -> updateForm { it.copy(categoryEditor = null) }
                    else -> {
                        updateEditor { categoryEditorReducer.saveFailed(it, result.error) }
                        if (!categoryEditorReducer.isNameProblem(result.error)) {
                            _effects.send(AddEditTransactionEffect.ShowError(result.error.toMessageRes()))
                        }
                    }
                }
            }
        }
    }

    /** Changes the new-category sheet's form, if the sheet is open. */
    private fun updateEditor(change: (CategoryEditorState) -> CategoryEditorState) {
        updateForm { it.copy(categoryEditor = it.categoryEditor?.let(change)) }
    }

    private fun save() {
        val state = _uiState.value
        val category = state.selectedCategory
        val amount = state.validAmount
        if (!state.isSaveEnabled || category == null || amount == null) return

        // Also disables Save, so a second tap can't store the transaction twice
        updateForm { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = saveTransaction(
                Transaction(
                    id = transactionId ?: TransactionId.UNSAVED,
                    title = state.title.trim(),
                    amount = amount,
                    note = state.note.trim(),
                    dateMillis = state.dateMillis,
                    categoryId = category.id,
                    categoryName = category.name,
                    transactionType = state.transactionType,
                    categoryIcon = category.icon
                )
            )

            result
                .onSuccess { _effects.send(AddEditTransactionEffect.Saved) }
                .onFailure { error ->
                    updateForm { it.copy(isSaving = false) }
                    _effects.send(AddEditTransactionEffect.ShowError(error.toMessageRes()))
                }
        }
    }

    private fun delete() {
        val transaction = _uiState.value.transactionToDelete ?: return

        updateForm { it.copy(transactionToDelete = null) }
        viewModelScope.launch {
            deleteTransaction(transaction)
                .onSuccess { _effects.send(AddEditTransactionEffect.NavigateBack) }
                .onFailure { _effects.send(AddEditTransactionEffect.ShowError(it.toMessageRes())) }
        }
    }

    /** Applies [change], then re-derives what depends on the whole form. */
    private fun updateForm(change: (AddEditTransactionUiState) -> AddEditTransactionUiState) {
        _uiState.update { change(it).validated() }
    }

    private fun AddEditTransactionUiState.validated(): AddEditTransactionUiState {
        val validation = validateTransactionDraft(
            amount = amount,
            title = title,
            note = note,
            hasCategory = selectedCategory != null
        )

        return copy(
            validAmount = validation.amount,
            isSaveEnabled = validation.isValid && !isSaving
        )
    }

    private companion object {
        const val NOTE_MAX_LENGTH = ValidateTransactionDraftUseCase.NOTE_MAX_LENGTH
        const val KEY_BILL_IMAGE = "bill_image_uri"
    }
}
