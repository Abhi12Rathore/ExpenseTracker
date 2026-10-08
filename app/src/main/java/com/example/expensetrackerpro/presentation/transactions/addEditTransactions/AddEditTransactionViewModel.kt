package com.example.expensetrackerpro.presentation.transactions.addEditTransactions


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.AddCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesByTypeUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.AddTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionByIdUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateTransactionInputUseCase
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEvent
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val getTransactionById: GetTransactionByIdUseCase,
    private val getCategories: GetCategoriesUseCase,
    getCategoriesByType: GetCategoriesByTypeUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val validateTransactionInput: ValidateTransactionInputUseCase,
    private val addCategory: AddCategoryUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Nav argument; -1 (the default) means "add" mode
    private val transactionId: Int? =
        savedStateHandle.get<Int>("transactionId")?.takeIf { it != -1 }

    private val isEditMode = transactionId != null

    // Everything the form owns; availableCategories is merged in below
    private val formState = MutableStateFlow(AddEditTransactionUiState(isEditMode = isEditMode))

    /**
     * SENIOR ARCHITECTURE: Reactive Category Stream
     * This observes the transactionType in formState. Whenever it changes,
     * flatMapLatest triggers a new Room query to fetch the correct categories.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val availableCategories: Flow<List<Category>> = formState
        .map { it.transactionType }
        .distinctUntilChanged()
        .flatMapLatest { type -> getCategoriesByType(type) }
        .onStart { emit(emptyList()) }

    val uiState: StateFlow<AddEditTransactionUiState> =
        combine(formState, availableCategories) { form, categories ->
            form.copy(availableCategories = categories)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = formState.value
        )

    init {
        if (transactionId != null) {
            loadExistingTransaction(transactionId)
        }
    }

    fun onEvent(event: AddEditTransactionEvent) {
        when (event) {
            is AddEditTransactionEvent.TypeChanged -> onTypeChange(event.type)
            is AddEditTransactionEvent.AmountChanged -> onAmountChange(event.amount)
            is AddEditTransactionEvent.NoteChanged -> onNoteChange(event.note)
            is AddEditTransactionEvent.CategorySelected -> onCategorySelect(event.category)
            is AddEditTransactionEvent.DateSelected -> onDateSelected(event.millis)
            is AddEditTransactionEvent.DatePickerToggled ->
                formState.update { it.copy(isDatePickerVisible = event.show) }
            AddEditTransactionEvent.CategorySheetOpened ->
                formState.update { it.copy(isCategorySheetVisible = true) }
            AddEditTransactionEvent.CategorySheetDismissed ->
                formState.update { it.copy(isCategorySheetVisible = false) }
            is AddEditTransactionEvent.CategoryAdded ->
                onCategoryAdded(event.name, event.iconName, event.type)
            AddEditTransactionEvent.SaveClicked -> saveTransaction()
        }
    }

    private fun loadExistingTransaction(id: Int) {
        viewModelScope.launch {
            val transaction = getTransactionById(id) ?: return@launch
            // We need to fetch the category object because transaction only stores categoryId
            val category = getCategories().first().find { it.id == transaction.categoryId }

            formState.update { it.copy(
                amount = transaction.amount.toString(),
                note = transaction.note,
                transactionType = transaction.transactionType,
                selectedCategory = category,
                dateMillis = transaction.dateMillis
            )}
            validate()
        }
    }

    private fun onTypeChange(newType: TransactionType) {
        formState.update {
            it.copy(
                transactionType = newType,
                selectedCategory = null // Reset category to prevent Income cat in Expense trans
            )
        }
        validate()
    }

    private fun onAmountChange(newAmount: String) {
        // Only allow numeric input
        if (newAmount.isEmpty() || newAmount.toDoubleOrNull() != null) {
            formState.update { it.copy(amount = newAmount) }
            validate()
        }
    }

    private fun onNoteChange(newNote: String) {
        formState.update { it.copy(note = newNote) }
        validate()
    }

    private fun onCategorySelect(category: Category) {
        formState.update { it.copy(selectedCategory = category) }
        validate()
    }

    private fun onDateSelected(millis: Long?) {
        millis?.let { selectedDate ->
            formState.update { it.copy(dateMillis = selectedDate, isDatePickerVisible = false) }
        }
    }

    private fun validate() {
        val state = formState.value
        val isValid = validateTransactionInput(
            amount = state.amount,
            note = state.note,
            hasCategory = state.selectedCategory != null
        )

        formState.update { it.copy(isSaveEnabled = isValid) }
    }

    private fun saveTransaction() {
        val currentState = formState.value
        val category = currentState.selectedCategory
        if (currentState.isSaveEnabled && category != null) {
            viewModelScope.launch {
                val transaction = Transaction(
                    id = transactionId ?: 0,
                    amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                    note = currentState.note,
                    dateMillis = currentState.dateMillis,
                    categoryId = category.id,
                    transactionType = currentState.transactionType,
                    title = currentState.note,
                    categoryName = category.name
                )

                if (isEditMode) {
                    updateTransaction(transaction)
                } else {
                    addTransaction(transaction)
                }
                onSuccess()
            }
        }
    }

    private fun onCategoryAdded(name: String, icon: String, type: TransactionType) {
        formState.update { it.copy(isCategorySheetVisible = false) }
        viewModelScope.launch {
            val newCategory = Category(
                name = name,
                iconName = icon,
                type = type, // Now passing the type from the UI context
                isCustom = true
            )
            addCategory(newCategory)
        }
    }

    private fun onSuccess() {
        // Reset state for new entry
        formState.update { AddEditTransactionUiState(isEditMode = isEditMode) }
    }
}
