package com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import com.example.expensetrackerpro.utils.ExpenseFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val repository: TransactionRepository
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
    val totalIncome = transactions.map { list ->
        list.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense = transactions.map { list ->
        list.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // The "Whole" part: Income - Expense
    val totalBalance = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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
                repository.deleteTransaction(transaction)
                _transactionToDelete.value = null
            }
        }
    }


}
