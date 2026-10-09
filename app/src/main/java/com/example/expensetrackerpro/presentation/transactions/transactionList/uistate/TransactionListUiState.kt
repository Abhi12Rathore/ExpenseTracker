package com.example.expensetrackerpro.presentation.transactions.transactionList.uistate

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.TransactionDayGroup
import com.example.expensetrackerpro.domain.models.TransactionListSummary
import com.example.expensetrackerpro.domain.models.TransactionTypeCounts
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.navigation.CategoryFilter

data class TransactionListUiState(
    // What every filter together leaves, newest day first
    val days: List<TransactionDayGroup> = emptyList(),
    val summary: TransactionListSummary = TransactionListSummary.Overview(),
    val counts: TransactionTypeCounts = TransactionTypeCounts(),
    val typeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val filter: PeriodFilterState = PeriodFilterState(),
    // Non-null when only one category's transactions are listed
    val categoryFilter: CategoryFilter? = null,
    val isSearchOpen: Boolean = false,
    // As typed; the list follows a moment later
    val searchQuery: String = "",
    // Changes when a swiped row could not be deleted and has to slide back
    val swipeResetToken: Int = 0
) {
    /** True once the list shows search results, which is a moment after the typing. */
    val isShowingSearchResults: Boolean get() = summary is TransactionListSummary.Search

    val isAllTime: Boolean get() = filter.dateRange == DateRange.ALL
}
