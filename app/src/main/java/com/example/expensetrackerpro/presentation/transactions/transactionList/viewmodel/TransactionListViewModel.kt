package com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.navigation.HistoryFilter
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEvent
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    getTransactions: GetTransactionsUseCase,
    private val getDateRange: GetDateRangeUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val stepPeriod: StepPeriodUseCase,
    private val getCustomDateRange: GetCustomDateRangeUseCase,
    private val calculateFinancialSummary: CalculateFinancialSummaryUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Set when History was opened from a dashboard chart
    private val openedWith = HistoryFilter.from(savedStateHandle)

    private val filter = MutableStateFlow(
        openedWith?.dateFilter ?: getCurrentPeriod(PeriodGranularity.MONTH)
    )

    private val categoryFilter = MutableStateFlow(openedWith?.category)

    private val filterState = filter.map { it.toState() }

    private val transactionToDelete = MutableStateFlow<Transaction?>(null)

    private val searchQuery = MutableStateFlow("")

    // Waits for a pause in typing before searching; clearing the field applies at once
    private val appliedSearchQuery = searchQuery
        .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .distinctUntilChanged()

    private val transactions = combine(filterState, appliedSearchQuery) { state, query -> state.dateRange to query }
        .flatMapLatest { (dateRange, query) -> getTransactions(dateRange, query) }
        .combine(categoryFilter) { list, category ->
            if (category == null) list else list.filter { it.categoryId.toLong() == category.id }
        }

    /**
     * Income, Expense, and Balance are derived from the filtered list reactively.
     */
    val uiState: StateFlow<TransactionListUiState> =
        combine(
            transactions,
            filterState,
            categoryFilter,
            searchQuery,
            transactionToDelete
        ) { list, filterState, category, query, toDelete ->
            val summary = calculateFinancialSummary(list)

            TransactionListUiState(
                transactions = list,
                filter = filterState,
                categoryFilter = category,
                searchQuery = query,
                totalIncome = summary.totalIncome,
                totalExpense = summary.totalExpense,
                totalBalance = summary.totalBalance,
                transactionToDelete = toDelete
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransactionListUiState(
                filter = filter.value.toState(),
                categoryFilter = categoryFilter.value
            )
        )

    fun onEvent(event: TransactionListEvent) {
        when (event) {
            is TransactionListEvent.Filter -> onFilterEvent(event.event)
            TransactionListEvent.CategoryFilterCleared -> categoryFilter.value = null
            is TransactionListEvent.SearchChanged -> searchQuery.value = event.query
            is TransactionListEvent.DeleteRequested -> transactionToDelete.value = event.transaction
            TransactionListEvent.DeleteDismissed -> transactionToDelete.value = null
            TransactionListEvent.DeleteConfirmed -> confirmDelete()
        }
    }

    private fun onFilterEvent(event: PeriodFilterEvent) {
        val current = filter.value

        filter.value = when (event) {
            // Tapping a segment always jumps to the period we are in now
            is PeriodFilterEvent.GranularitySelected -> getCurrentPeriod(event.granularity)
            PeriodFilterEvent.PreviousPeriod -> current.stepped(forward = false)
            PeriodFilterEvent.NextPeriod -> current.stepped(forward = true)
            is PeriodFilterEvent.CustomRangeSelected -> DateFilter.Custom(
                getCustomDateRange(event.startUtcMillis, event.endUtcMillis)
            )
        }
    }

    /** The neighbouring period, or the filter unchanged when there is nowhere to step to. */
    private fun DateFilter.stepped(forward: Boolean): DateFilter =
        (this as? DateFilter.Period)?.let { stepPeriod(it, forward) } ?: this

    private fun DateFilter.toState() = PeriodFilterState(
        filter = this,
        dateRange = getDateRange(this),
        canStepForward = this is DateFilter.Period && stepPeriod(this, forward = true) != null
    )

    private fun confirmDelete() {
        transactionToDelete.value?.let { transaction ->
            viewModelScope.launch {
                deleteTransaction(transaction)
                transactionToDelete.value = null
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
