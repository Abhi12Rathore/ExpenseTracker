package com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.error.onFailure
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionListingUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.navigation.TransactionsFilter
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEffect
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEvent
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
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
    private val getTransactionListing: GetTransactionListingUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Set when Transactions was opened from a dashboard chart
    private val openedWith = TransactionsFilter.from(savedStateHandle)

    private val filter = MutableStateFlow(
        openedWith?.dateFilter ?: getCurrentPeriod(PeriodGranularity.MONTH)
    )

    private val categoryFilter = MutableStateFlow(openedWith?.category)

    private val typeFilter = MutableStateFlow(TransactionTypeFilter.ALL)

    private val filterState = filter.map { it.toState() }

    private val isSearchOpen = MutableStateFlow(false)

    private val searchQuery = MutableStateFlow("")

    private val swipeResetToken = MutableStateFlow(0)

    // Deletes that have been asked for and have not finished yet
    private val pendingDeleteIds = mutableSetOf<Int>()

    // A Channel, not state: each effect is handled once, and waits if nobody is collecting yet
    private val _effects = Channel<TransactionListEffect>(Channel.BUFFERED)
    val effects: Flow<TransactionListEffect> = _effects.receiveAsFlow()

    // Waits for a pause in typing before searching; clearing the field applies at once
    private val appliedSearchQuery = searchQuery
        .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .distinctUntilChanged()

    // The period's transactions, paired with the search that narrowed them
    private val searchedTransactions =
        combine(filterState, appliedSearchQuery) { state, query -> state.dateRange to query }
            .flatMapLatest { (dateRange, query) ->
                getTransactions(dateRange, query).map { list -> list to query }
            }

    private val listing = combine(searchedTransactions, typeFilter, categoryFilter) { (list, query), type, category ->
        getTransactionListing(
            transactions = list,
            typeFilter = type,
            categoryId = category?.id,
            isSearching = query.isNotBlank()
        )
    }

    private val search = combine(isSearchOpen, searchQuery) { isOpen, query -> isOpen to query }

    private val listFilters = combine(typeFilter, categoryFilter) { type, category -> type to category }

    val uiState: StateFlow<TransactionListUiState> =
        combine(
            listing,
            filterState,
            listFilters,
            search,
            swipeResetToken
        ) { listing, filterState, (type, category), (isSearchOpen, query), resetToken ->
            TransactionListUiState(
                days = listing.days,
                summary = listing.summary,
                counts = listing.counts,
                typeFilter = type,
                filter = filterState,
                categoryFilter = category,
                isSearchOpen = isSearchOpen,
                searchQuery = query,
                swipeResetToken = resetToken
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
            is TransactionListEvent.TypeFilterSelected -> typeFilter.value = event.typeFilter
            TransactionListEvent.CategoryFilterCleared -> categoryFilter.value = null
            TransactionListEvent.SearchOpened -> isSearchOpen.value = true
            TransactionListEvent.SearchClosed -> {
                isSearchOpen.value = false
                searchQuery.value = ""
            }
            is TransactionListEvent.SearchChanged -> searchQuery.value = event.query
            TransactionListEvent.SearchAllTime -> filter.value = DateFilter.Custom(DateRange.ALL)
            is TransactionListEvent.DeleteSwiped -> delete(event.transaction)
        }
    }

    private fun onFilterEvent(event: PeriodFilterEvent) {
        val current = filter.value

        filter.value = when (event) {
            // Choosing a granularity always jumps to the period we are in now
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

    private fun delete(transaction: Transaction) {
        // A swipe can be reported twice. Only a transaction that is listed and not already on
        // its way out is deleted, so each swipe leads to one delete and one Undo.
        val isListed = uiState.value.days.any { day -> day.transactions.any { it.id == transaction.id } }
        if (!isListed || !pendingDeleteIds.add(transaction.id)) return

        viewModelScope.launch {
            try {
                deleteTransaction(transaction).onFailure { error ->
                    // The row is still swiped aside, so it has to be brought back
                    swipeResetToken.update { it + 1 }
                    _effects.send(TransactionListEffect.DeleteFailed(error.toMessageRes()))
                }
            } finally {
                pendingDeleteIds.remove(transaction.id)
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
