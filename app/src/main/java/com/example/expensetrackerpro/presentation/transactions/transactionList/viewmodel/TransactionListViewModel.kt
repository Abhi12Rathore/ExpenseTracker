package com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEvent
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListUiState
import com.example.expensetrackerpro.utils.ExpenseFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    getTransactions: GetTransactionsUseCase,
    private val calculateFinancialSummary: CalculateFinancialSummaryUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(ExpenseFilter.ALL)

    private val transactionToDelete = MutableStateFlow<Transaction?>(null)

    private val transactions = selectedFilter
        .flatMapLatest { filter -> getTransactions(filter.getStartTimestamp()) }

    /**
     * Income, Expense, and Balance are derived from the filtered list reactively.
     */
    val uiState: StateFlow<TransactionListUiState> =
        combine(transactions, selectedFilter, transactionToDelete) { list, filter, toDelete ->
            val summary = calculateFinancialSummary(list)

            TransactionListUiState(
                transactions = list,
                selectedFilter = filter,
                totalIncome = summary.totalIncome,
                totalExpense = summary.totalExpense,
                totalBalance = summary.totalBalance,
                transactionToDelete = toDelete
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransactionListUiState()
        )

    fun onEvent(event: TransactionListEvent) {
        when (event) {
            is TransactionListEvent.FilterSelected -> selectedFilter.value = event.filter
            is TransactionListEvent.DeleteRequested -> transactionToDelete.value = event.transaction
            TransactionListEvent.DeleteDismissed -> transactionToDelete.value = null
            TransactionListEvent.DeleteConfirmed -> confirmDelete()
        }
    }

    private fun confirmDelete() {
        transactionToDelete.value?.let { transaction ->
            viewModelScope.launch {
                deleteTransaction(transaction)
                transactionToDelete.value = null
            }
        }
    }
}
