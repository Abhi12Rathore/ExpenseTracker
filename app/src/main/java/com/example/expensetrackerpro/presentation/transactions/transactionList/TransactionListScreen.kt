package com.example.expensetrackerpro.presentation.transactions.transactionList

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.TransactionListSummary
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.TransactionTypeCounts
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.common.filter.components.CompactPeriodFilter
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.DayHeader
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.RowPosition
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.TransactionRow
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.TransactionSummaryStrip
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.TransactionTypeChips
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.TransactionsEmptyState
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.TransactionsTopBar
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEffect
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEvent
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListUiState
import com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel.TransactionListViewModel

@Composable
fun TransactionListScreen(
    // The app's shared host, so a message here looks and queues like the Undo one
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onTransactionClick: (Int) -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deleteFailedMessage = stringResource(R.string.transaction_delete_failed)

    LaunchedEffect(viewModel, deleteFailedMessage) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TransactionListEffect.DeleteFailed -> snackbarHostState.showSnackbar(deleteFailedMessage)
            }
        }
    }

    // Back leaves the search before it leaves the screen
    BackHandler(enabled = uiState.isSearchOpen) {
        viewModel.onEvent(TransactionListEvent.SearchClosed)
    }

    TransactionListContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onAddClick = onAddClick,
        onTransactionClick = onTransactionClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListContent(
    uiState: TransactionListUiState,
    onEvent: (TransactionListEvent) -> Unit,
    onAddClick: () -> Unit,
    onTransactionClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val isFabExpanded = listState.isScrollingUp()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TransactionsTopBar(
                isSearchOpen = uiState.isSearchOpen,
                searchQuery = uiState.searchQuery,
                onOpenSearch = { onEvent(TransactionListEvent.SearchOpened) },
                onSearchChange = { onEvent(TransactionListEvent.SearchChanged(it)) },
                onCloseSearch = { onEvent(TransactionListEvent.SearchClosed) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.transactions_add)) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        // Collapsed, the icon is all that is left to name the button
                        contentDescription = if (isFabExpanded) null else stringResource(R.string.transactions_add)
                    )
                },
                onClick = onAddClick,
                expanded = isFabExpanded,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // The bottom leaves room to scroll the last row clear of the FAB
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)
        ) {
            item(key = KEY_PERIOD) {
                CompactPeriodFilter(
                    state = uiState.filter,
                    onEvent = { onEvent(TransactionListEvent.Filter(it)) }
                )
            }

            item(key = KEY_SUMMARY) {
                TransactionSummaryStrip(
                    summary = uiState.summary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            item(key = KEY_TYPE_CHIPS) {
                TransactionTypeChips(
                    selected = uiState.typeFilter,
                    counts = uiState.counts,
                    categoryFilter = uiState.categoryFilter,
                    onSelect = { onEvent(TransactionListEvent.TypeFilterSelected(it)) },
                    onClearCategory = { onEvent(TransactionListEvent.CategoryFilterCleared) },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.days.isEmpty()) {
                item(key = KEY_EMPTY) {
                    TransactionsEmptyState(
                        uiState = uiState,
                        onSearchAllTime = { onEvent(TransactionListEvent.SearchAllTime) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            uiState.days.forEach { day ->
                stickyHeader(key = "day-${day.dayStartMillis}", contentType = CONTENT_TYPE_HEADER) {
                    DayHeader(day = day, modifier = Modifier.animateItem())
                }

                // One item per row rather than per card, so a row can animate in and out by itself
                itemsIndexed(
                    items = day.transactions,
                    key = { _, transaction -> transaction.id },
                    contentType = { _, _ -> CONTENT_TYPE_ROW }
                ) { index, transaction ->
                    TransactionRow(
                        transaction = transaction,
                        position = RowPosition.of(index, day.transactions.size),
                        searchQuery = uiState.searchQuery,
                        swipeResetToken = uiState.swipeResetToken,
                        onClick = { onTransactionClick(transaction.id) },
                        onDelete = { onEvent(TransactionListEvent.DeleteSwiped(transaction)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/** False while the list is being scrolled towards its end, true otherwise. */
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }

    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

// Row keys are transaction ids, so everything else in the list is keyed by name
private const val KEY_PERIOD = "period"
private const val KEY_SUMMARY = "summary"
private const val KEY_TYPE_CHIPS = "typeChips"
private const val KEY_EMPTY = "empty"
private const val CONTENT_TYPE_HEADER = "dayHeader"
private const val CONTENT_TYPE_ROW = "transactionRow"

// October 2025, the month PreviewData is set in
private val previewFilter = PeriodFilterState(
    filter = DateFilter.Period(PeriodGranularity.MONTH, PreviewData.DATE_MILLIS),
    dateRange = DateRange(PreviewData.DATE_MILLIS, PreviewData.DATE_MILLIS + 1),
    canStepForward = false
)

@PreviewLightDark
@Composable
private fun TransactionListContentPreview() {
    ExpenseTrackerProTheme {
        TransactionListContent(
            uiState = TransactionListUiState(
                days = PreviewData.transactionDays,
                summary = TransactionListSummary.Overview(
                    income = Money.ofRupees(85_000),
                    expense = Money.ofRupees(4_650),
                    net = Money.ofRupees(80_350)
                ),
                counts = TransactionTypeCounts(all = 4, expense = 3, income = 1),
                filter = previewFilter
            ),
            onEvent = {},
            onAddClick = {},
            onTransactionClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TransactionListIncomeFilterPreview() {
    ExpenseTrackerProTheme {
        TransactionListContent(
            uiState = TransactionListUiState(
                days = PreviewData.transactionDays.subList(1, 2),
                summary = TransactionListSummary.SingleType(
                    type = TransactionType.INCOME,
                    total = Money.ofRupees(85_000),
                    count = 1,
                    biggest = Money.ofRupees(85_000)
                ),
                counts = TransactionTypeCounts(all = 4, expense = 3, income = 1),
                typeFilter = TransactionTypeFilter.INCOME,
                filter = previewFilter
            ),
            onEvent = {},
            onAddClick = {},
            onTransactionClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TransactionListSearchPreview() {
    ExpenseTrackerProTheme {
        TransactionListContent(
            uiState = TransactionListUiState(
                // "ca" is in the note "…plus the cake…" and in the title "Metro card"
                days = PreviewData.transactionDays.take(1) + PreviewData.transactionDays.takeLast(1),
                summary = TransactionListSummary.Search(
                    count = 3,
                    total = Money.ofRupees(-4_650),
                    average = Money.ofRupees(-1_550)
                ),
                counts = TransactionTypeCounts(all = 3, expense = 3, income = 0),
                filter = previewFilter,
                isSearchOpen = true,
                searchQuery = "ca"
            ),
            onEvent = {},
            onAddClick = {},
            onTransactionClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TransactionListEmptyPreview() {
    ExpenseTrackerProTheme {
        TransactionListContent(
            uiState = TransactionListUiState(
                summary = TransactionListSummary.SingleType(
                    type = TransactionType.INCOME,
                    total = Money.ZERO,
                    count = 0,
                    biggest = Money.ZERO
                ),
                typeFilter = TransactionTypeFilter.INCOME,
                filter = previewFilter
            ),
            onEvent = {},
            onAddClick = {},
            onTransactionClick = {}
        )
    }
}
