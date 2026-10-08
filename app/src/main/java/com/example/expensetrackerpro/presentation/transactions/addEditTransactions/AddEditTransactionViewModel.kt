package com.example.expensetrackerpro.presentation.transactions.addEditTransactions


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.data.local.entity.Category
import com.example.expensetrackerpro.data.local.entity.Transaction
import com.example.expensetrackerpro.data.local.entity.TransactionType
import com.example.expensetrackerpro.data.repository.expenseRepositoryImpl.TransactionRepository
import com.example.expensetrackerpro.domain.models.AddTransactionUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Nav argument; -1 (the default) means "add" mode
    private val transactionId: Int? =
        savedStateHandle.get<Int>("transactionId")?.takeIf { it != -1 }

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    val isEditMode = transactionId != null

    init {
        if (isEditMode) {
            loadExistingTransaction()
        }
    }

    /**
     * SENIOR ARCHITECTURE: Reactive Category Stream
     * This observes the transactionType in _uiState. Whenever it changes,
     * flatMapLatest triggers a new Room query to fetch the correct categories.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val availableCategories: StateFlow<List<Category>> = _uiState
        .map { it.transactionType }
        .distinctUntilChanged()
        .flatMapLatest { type ->
            repository.getCategoriesByType(type)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun loadExistingTransaction() {
        viewModelScope.launch {
            repository.getTransactionById(transactionId!!).let { transaction ->
                if (transaction != null) {
                    // We need to fetch the category object because transaction only stores categoryId
                    val allCats = repository.getAllCategories().first()
                    val category = allCats.find { it.id == transaction.categoryId }

                    _uiState.update { it.copy(
                        amount = transaction.amount.toString(),
                        note = transaction.note,
                        transactionType = transaction.transactionType,
                        selectedCategory = category,
                        dateMillis = transaction.dateMillis
                    )}
                    validate()
                }
            }
        }
    }

    fun onTypeChange(newType: TransactionType) {
        _uiState.update {
            it.copy(
                transactionType = newType,
                selectedCategory = null // Reset category to prevent Income cat in Expense trans
            )
        }
        validate()
    }

    fun onAmountChange(newAmount: String) {
        // Only allow numeric input
        if (newAmount.isEmpty() || newAmount.toDoubleOrNull() != null) {
            _uiState.update { it.copy(amount = newAmount) }
            validate()
        }
    }

    fun onNoteChange(newNote: String) {
        _uiState.update { it.copy(note = newNote) }
        validate()
    }

    fun onCategorySelect(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
        validate()
    }

    fun onDateSelected(millis: Long?) {
        millis?.let { selectedDate ->
            _uiState.update { it.copy(dateMillis = selectedDate, isDatePickerVisible = false) }
        }
    }

    fun toggleDatePicker(show: Boolean) {
        _uiState.update { it.copy(isDatePickerVisible = show) }
    }

    private fun validate() {
        val state = _uiState.value
        val amountDouble = state.amount.toDoubleOrNull() ?: 0.0
        val isValid = state.selectedCategory != null &&
                state.note.isNotBlank() &&
                amountDouble > 0.0

        _uiState.update { it.copy(isSaveEnabled = isValid) }
    }

    fun saveTransaction() {
        val currentState = _uiState.value
        if (currentState.isSaveEnabled) {
            viewModelScope.launch {
                val transaction = Transaction(
                    id = transactionId ?: 0,
                    amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                    note = currentState.note,
                    dateMillis = currentState.dateMillis,
                    categoryId = currentState.selectedCategory!!.id,
                    transactionType = currentState.transactionType,
                    title = currentState.note,
                    categoryName = currentState.selectedCategory.name
                )

                if (isEditMode) {
                    repository.updateTransaction(transaction)
                } else {
                    repository.saveTransaction(transaction)
                }
                onSuccess()
            }
        }
    }

    fun addCategory(name: String, icon: String, type: TransactionType) {
        viewModelScope.launch {
            val newCategory = Category(
                name = name,
                iconName = icon,
                type = type, // Now passing the type from the UI context
                isCustom = true
            )
            repository.insertCategory(newCategory)
        }
    }

    private fun onSuccess() {
        // Reset state for new entry
        _uiState.update { AddTransactionUiState() }
    }
}