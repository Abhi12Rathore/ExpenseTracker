package com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.utils.ExpenseFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val calculateFinancialSummary: CalculateFinancialSummaryUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(ExpenseFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    /**
     * Unified Transactions Flow:
     * Now fetches Transactions instead of Expenses.
     */
    val transactions: StateFlow<List<Transaction>> = _selectedFilter
        .flatMapLatest { filter ->
            repository.getFilteredTransactions(filter.getStartTimestamp())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Financial Summary State:
     * We derive Income, Expense, and Balance from the filtered list reactively.
     */
    private val summary = transactions
        .map { calculateFinancialSummary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    val totalIncome = summary.map { it.totalIncome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense = summary.map { it.totalExpense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // The "Whole" part: Income - Expense
    val totalBalance = summary.map { it.totalBalance }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun onFilterSelected(filter: ExpenseFilter) {
        _selectedFilter.value = filter
    }

    // --- Deletion Logic ---
    private val _transactionToDelete = MutableStateFlow<Transaction?>(null)
    val transactionToDelete = _transactionToDelete.asStateFlow()

    fun showDeleteConfirmation(transaction: Transaction) {
        _transactionToDelete.value = transaction
    }

    fun dismissDeleteDialog() {
        _transactionToDelete.value = null
    }

    fun confirmDelete() {
        _transactionToDelete.value?.let { transaction ->
            viewModelScope.launch {
                deleteTransaction(transaction)
                _transactionToDelete.value = null
            }
        }
    }


}
