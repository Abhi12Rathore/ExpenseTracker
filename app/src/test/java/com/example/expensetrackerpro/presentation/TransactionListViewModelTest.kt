package com.example.expensetrackerpro.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionListSummary
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.TransactionTypeCounts
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.FilterTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionListingUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GroupTransactionsByDayUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.SummarizeTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.fakes.rupees
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertFalse
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertTrue
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.navigation.CategoryFilter
import com.example.expensetrackerpro.presentation.navigation.Screens
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEffect
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEvent
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListUiState
import com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel.TransactionListViewModel
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Thursday 8 Oct 2026
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)
    private val timeProvider = FakeTimeProvider(now)
    private val salary = transaction(id = 1, amount = rupees(1000), type = TransactionType.INCOME, dateMillis = now)
    private val lunch = transaction(
        id = 2,
        amount = rupees(250),
        type = TransactionType.EXPENSE,
        dateMillis = millisAt(2026, Calendar.OCTOBER, 2)
    )
    private val oldRent = transaction(
        id = 3,
        amount = rupees(400),
        type = TransactionType.EXPENSE,
        dateMillis = millisAt(2026, Calendar.SEPTEMBER, 30, hour = 23)
    )

    private val repository = FakeTransactionRepository(listOf(salary, lunch, oldRent))

    private val october =
        DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1))
    private val thisMonth = PeriodFilterState(
        filter = DateFilter.Period(PeriodGranularity.MONTH, october.startMillis),
        dateRange = october,
        canStepForward = false
    )

    private fun viewModel(
        repository: FakeTransactionRepository = this.repository,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) =
        TransactionListViewModel(
            getTransactions = GetTransactionsUseCase(repository),
            getDateRange = GetDateRangeUseCase(timeProvider),
            getCurrentPeriod = GetCurrentPeriodUseCase(timeProvider),
            stepPeriod = StepPeriodUseCase(timeProvider),
            getCustomDateRange = GetCustomDateRangeUseCase(timeProvider),
            getTransactionListing = GetTransactionListingUseCase(
                FilterTransactionsUseCase(),
                GroupTransactionsByDayUseCase(timeProvider),
                SummarizeTransactionsUseCase()
            ),
            deleteTransaction = DeleteTransactionUseCase(repository),
            savedStateHandle = savedStateHandle
        )

    private fun TransactionListViewModel.onFilterEvent(event: PeriodFilterEvent) =
        onEvent(TransactionListEvent.Filter(event))

    // Everything listed, in the order it is shown
    private val TransactionListUiState.transactions: List<Transaction>
        get() = days.flatMap { it.transactions }

    private val TransactionListUiState.overview: TransactionListSummary.Overview
        get() = summary as TransactionListSummary.Overview

    @Test
    fun emptyRepository_isEmptyStateForThisMonth() = runTest {
        val viewModel = viewModel(FakeTransactionRepository())
        subscribe(viewModel.uiState)

        assertEquals(TransactionListUiState(filter = thisMonth), viewModel.uiState.value)
    }

    @Test
    fun beforeAnyoneSubscribes_alreadyShowsThisMonth() {
        assertEquals(thisMonth, viewModel().uiState.value.filter)
    }

    @Test
    fun byDefault_showsThisMonthNewestFirstWithTotals() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertEquals(thisMonth, state.filter)
        assertEquals(listOf(salary, lunch), state.transactions)
        assertEquals(rupees(1000), state.overview.income)
        assertEquals(rupees(250), state.overview.expense)
        assertEquals(rupees(750), state.overview.net)
    }

    @Test
    fun previousPeriod_showsLastMonth_andNextComesBack() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        val september = viewModel.uiState.value

        assertEquals(
            DateRange(millisAt(2026, Calendar.SEPTEMBER, 1), millisAt(2026, Calendar.OCTOBER, 1)),
            september.filter.dateRange
        )
        assertEquals(true, september.filter.canStepForward)
        assertEquals(listOf(oldRent), september.transactions)
        assertEquals(rupees(-400), september.overview.net)

        viewModel.onFilterEvent(PeriodFilterEvent.NextPeriod)
        assertEquals(thisMonth, viewModel.uiState.value.filter)
        assertEquals(listOf(salary, lunch), viewModel.uiState.value.transactions)
    }

    @Test
    fun nextPeriod_atTheCurrentMonth_doesNothing() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.NextPeriod)

        assertEquals(thisMonth, viewModel.uiState.value.filter)
    }

    @Test
    fun granularitySelected_narrowsToThisWeekOrWidensToThisYear() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.WEEK))
        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 11)),
            viewModel.uiState.value.filter.dateRange
        )
        assertEquals(listOf(salary), viewModel.uiState.value.transactions)

        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.YEAR))
        assertEquals(listOf(salary, lunch, oldRent), viewModel.uiState.value.transactions)
        assertEquals(rupees(350), viewModel.uiState.value.overview.net)
    }

    @Test
    fun customRangeSelected_showsThePickedDaysInclusive() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val utc = TimeZone.getTimeZone("UTC")

        // 30 Sep to 2 Oct, as a date picker reports them
        viewModel.onFilterEvent(
            PeriodFilterEvent.CustomRangeSelected(
                startUtcMillis = millisAt(2026, Calendar.SEPTEMBER, 30, timeZone = utc),
                endUtcMillis = millisAt(2026, Calendar.OCTOBER, 2, timeZone = utc)
            )
        )
        val state = viewModel.uiState.value
        val picked =
            DateRange(millisAt(2026, Calendar.SEPTEMBER, 30), millisAt(2026, Calendar.OCTOBER, 3))

        assertEquals(
            PeriodFilterState(filter = DateFilter.Custom(picked), dateRange = picked, canStepForward = false),
            state.filter
        )
        assertEquals(listOf(lunch, oldRent), state.transactions)
        assertEquals(rupees(650), state.overview.expense)

        // The arrows have nothing to step in a custom range
        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        assertEquals(state.filter, viewModel.uiState.value.filter)
    }

    // --- Opened from a dashboard chart ---

    @Test
    fun openedWithADay_showsOnlyThatDay_asACustomRange() = runTest {
        val secondOfOctober =
            DateRange(millisAt(2026, Calendar.OCTOBER, 2), millisAt(2026, Calendar.OCTOBER, 3))
        val viewModel = viewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screens.Transactions.ARG_START to secondOfOctober.startMillis,
                    Screens.Transactions.ARG_END to secondOfOctober.endMillis
                )
            )
        )
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertEquals(
            PeriodFilterState(
                filter = DateFilter.Custom(secondOfOctober),
                dateRange = secondOfOctober,
                canStepForward = false
            ),
            state.filter
        )
        assertEquals(listOf(lunch), state.transactions)
        assertNull(state.categoryFilter)
    }

    @Test
    fun openedWithAMonth_showsThatMonth_andCanStillBeStepped() = runTest {
        val viewModel = viewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screens.Transactions.ARG_START to millisAt(2026, Calendar.SEPTEMBER, 1),
                    Screens.Transactions.ARG_GRANULARITY to PeriodGranularity.MONTH.name
                )
            )
        )
        subscribe(viewModel.uiState)

        assertEquals(listOf(oldRent), viewModel.uiState.value.transactions)
        assertEquals(true, viewModel.uiState.value.filter.canStepForward)

        viewModel.onFilterEvent(PeriodFilterEvent.NextPeriod)
        assertEquals(thisMonth, viewModel.uiState.value.filter)
    }

    @Test
    fun openedWithACategory_listsOnlyThatCategory_untilTheFilterIsCleared() = runTest {
        val travel = lunch.copy(id = 4, categoryId = 7, categoryName = "Travel", amount = rupees(90))
        val repository = FakeTransactionRepository(listOf(salary, lunch, oldRent, travel))
        val viewModel = viewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screens.Transactions.ARG_START to october.startMillis,
                    Screens.Transactions.ARG_GRANULARITY to PeriodGranularity.MONTH.name,
                    Screens.Transactions.ARG_CATEGORY_ID to 7L,
                    Screens.Transactions.ARG_CATEGORY_NAME to "Travel"
                )
            )
        )
        subscribe(viewModel.uiState)
        val filtered = viewModel.uiState.value

        assertEquals(thisMonth, filtered.filter)
        assertEquals(CategoryFilter(id = 7, name = "Travel"), filtered.categoryFilter)
        assertEquals(listOf(travel), filtered.transactions)
        // Totals follow what is listed
        assertEquals(rupees(90), filtered.overview.expense)
        assertEquals(Money.ZERO, filtered.overview.income)

        viewModel.onEvent(TransactionListEvent.CategoryFilterCleared)
        val cleared = viewModel.uiState.value

        assertNull(cleared.categoryFilter)
        assertEquals(listOf(salary, lunch, travel), cleared.transactions)
    }

    @Test
    fun categoryFilter_staysWhenThePeriodChanges() = runTest {
        val viewModel = viewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screens.Transactions.ARG_START to october.startMillis,
                    Screens.Transactions.ARG_GRANULARITY to PeriodGranularity.MONTH.name,
                    Screens.Transactions.ARG_CATEGORY_ID to 1L,
                    Screens.Transactions.ARG_CATEGORY_NAME to "Food"
                )
            )
        )
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)

        assertEquals(CategoryFilter(id = 1, name = "Food"), viewModel.uiState.value.categoryFilter)
        assertEquals(listOf(oldRent), viewModel.uiState.value.transactions)
    }

    @Test
    fun openedWithTheRoutesDefaults_isThePlainTransactions() = runTest {
        val viewModel = viewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screens.Transactions.ARG_START to Screens.Transactions.NO_VALUE,
                    Screens.Transactions.ARG_END to Screens.Transactions.NO_VALUE,
                    Screens.Transactions.ARG_CATEGORY_ID to Screens.Transactions.NO_VALUE
                )
            )
        )
        subscribe(viewModel.uiState)

        assertEquals(thisMonth, viewModel.uiState.value.filter)
        assertNull(viewModel.uiState.value.categoryFilter)
        assertEquals(listOf(salary, lunch), viewModel.uiState.value.transactions)
    }

    // --- Days, type filter and summary

    @Test
    fun byDefault_groupsByDay_withTodayNamedAndEachDaysNet() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val days = viewModel.uiState.value.days

        assertEquals(
            listOf(millisAt(2026, Calendar.OCTOBER, 8), millisAt(2026, Calendar.OCTOBER, 2)),
            days.map { it.dayStartMillis }
        )
        assertEquals(listOf(DayLabel.TODAY, DayLabel.OTHER), days.map { it.label })
        assertEquals(listOf(rupees(1000), rupees(-250)), days.map { it.net })
        assertEquals(TransactionTypeCounts(all = 2, expense = 1, income = 1), viewModel.uiState.value.counts)
    }

    @Test
    fun typeFilter_listsOnlyThatType_andKeepsEveryCount() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.TypeFilterSelected(TransactionTypeFilter.INCOME))
        val income = viewModel.uiState.value

        assertEquals(TransactionTypeFilter.INCOME, income.typeFilter)
        assertEquals(listOf(salary), income.transactions)
        assertEquals(TransactionTypeCounts(all = 2, expense = 1, income = 1), income.counts)
        assertEquals(
            TransactionListSummary.SingleType(TransactionType.INCOME, total = rupees(1000), count = 1, biggest = rupees(1000)),
            income.summary
        )

        viewModel.onEvent(TransactionListEvent.TypeFilterSelected(TransactionTypeFilter.EXPENSE))
        assertEquals(listOf(lunch), viewModel.uiState.value.transactions)

        viewModel.onEvent(TransactionListEvent.TypeFilterSelected(TransactionTypeFilter.ALL))
        assertEquals(listOf(salary, lunch), viewModel.uiState.value.transactions)
    }

    @Test
    fun typeFilter_staysWhenThePeriodChanges() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.TypeFilterSelected(TransactionTypeFilter.INCOME))
        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        val september = viewModel.uiState.value

        // September only has the rent
        assertTrue(september.transactions.isEmpty())
        assertEquals(TransactionTypeCounts(all = 1, expense = 1, income = 0), september.counts)
    }

    @Test
    fun typeFilter_combinesWithPeriodSearchAndCategory() = runTest {
        val travelRefund = salary.copy(id = 4, title = "Trip refund", amount = rupees(60), categoryId = 7, categoryName = "Travel")
        val travelTaxi = lunch.copy(id = 5, title = "Trip taxi", amount = rupees(90), categoryId = 7, categoryName = "Travel")
        val foodTrip = lunch.copy(id = 6, title = "Trip snacks", amount = rupees(30))
        val oldTravel = oldRent.copy(id = 7, title = "Trip hotel", categoryId = 7, categoryName = "Travel")
        val viewModel = viewModel(
            repository = FakeTransactionRepository(listOf(salary, lunch, travelRefund, travelTaxi, foodTrip, oldTravel)),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screens.Transactions.ARG_START to october.startMillis,
                    Screens.Transactions.ARG_GRANULARITY to PeriodGranularity.MONTH.name,
                    Screens.Transactions.ARG_CATEGORY_ID to 7L,
                    Screens.Transactions.ARG_CATEGORY_NAME to "Travel"
                )
            )
        )
        subscribe(viewModel.uiState)

        viewModel.search("trip")
        advanceTimeBy(301)
        runCurrent()
        viewModel.onEvent(TransactionListEvent.TypeFilterSelected(TransactionTypeFilter.EXPENSE))
        val state = viewModel.uiState.value

        // In October, matching "trip", in Travel, and an expense
        assertEquals(listOf(travelTaxi), state.transactions)
        assertEquals(TransactionTypeCounts(all = 2, expense = 1, income = 1), state.counts)
        assertEquals(TransactionListSummary.Search(count = 1, total = rupees(-90), average = rupees(-90)), state.summary)
    }

    // --- Swipe to delete

    @Test
    fun deleteSwiped_deletesAtOnce_andReportsItForTheUndo() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val deleted = mutableListOf<Transaction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.deletions.collect(deleted::add) }

        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))
        val state = viewModel.uiState.value

        assertEquals(listOf(salary), state.transactions)
        assertEquals(Money.ZERO, state.overview.expense)
        assertEquals(listOf(salary, oldRent), repository.current.sortedBy { it.id })
        assertEquals(listOf(lunch), deleted)
        assertEquals(0, state.swipeResetToken)
    }

    @Test
    fun deleteSwiped_twiceForTheSameTransaction_deletesOnce_andOffersOneUndo() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val deleted = mutableListOf<Transaction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.deletions.collect(deleted::add) }

        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))
        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))

        assertEquals(1, repository.deleteCalls)
        assertEquals(listOf(lunch), deleted)
        assertEquals(listOf(salary), viewModel.uiState.value.transactions)
        assertEquals(0, viewModel.uiState.value.swipeResetToken)
    }

    @Test
    fun deleteSwiped_twiceWhileTheFirstIsStillRunning_deletesOnce() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        // Holds the first delete open, as a slow database would
        repository.deleteGate = CompletableDeferred()

        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))
        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))
        repository.deleteGate?.complete(Unit)
        runCurrent()

        assertEquals(1, repository.deleteCalls)
        assertEquals(listOf(salary), viewModel.uiState.value.transactions)
    }

    @Test
    fun deleteSwiped_again_afterTheTransactionWasRestored_deletesItAgain() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))
        repository.saveTransaction(lunch)
        assertEquals(listOf(salary, lunch), viewModel.uiState.value.transactions)

        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))

        assertEquals(2, repository.deleteCalls)
        assertEquals(listOf(salary), viewModel.uiState.value.transactions)
    }

    @Test
    fun deleteSwiped_whenTheWriteFails_canBeTriedAgain() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        repository.failWrites = true
        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))
        repository.failWrites = false

        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))

        assertEquals(listOf(salary), viewModel.uiState.value.transactions)
    }

    @Test
    fun deleteSwiped_whenTheWriteFails_keepsTheRow_andAsksForItBack() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        repository.failWrites = true

        viewModel.onEvent(TransactionListEvent.DeleteSwiped(lunch))

        assertEquals(TransactionListEffect.DeleteFailed, viewModel.effects.first())
        assertEquals(listOf(salary, lunch), viewModel.uiState.value.transactions)
        assertEquals(1, viewModel.uiState.value.swipeResetToken)
    }

    // --- Search

    private val teamLunch = lunch.copy(title = "Team lunch", note = "Paid for Riya", categoryName = "Food")
    private val pay = salary.copy(title = "October pay", note = "", categoryName = "Salary")
    private val searchable get() = FakeTransactionRepository(listOf(pay, teamLunch, oldRent))

    private fun TransactionListViewModel.search(query: String) =
        onEvent(TransactionListEvent.SearchChanged(query))

    @Test
    fun search_showsTheQueryAtOnce_andFiltersAfterThreeHundredMillis() = runTest {
        val viewModel = viewModel(searchable)
        subscribe(viewModel.uiState)

        viewModel.search("lunch")
        advanceTimeBy(299)
        runCurrent()

        assertEquals("lunch", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(pay, teamLunch), viewModel.uiState.value.transactions)

        advanceTimeBy(2)
        runCurrent()

        assertEquals(listOf(teamLunch), viewModel.uiState.value.transactions)
        assertEquals(
            TransactionListSummary.Search(count = 1, total = rupees(-250), average = rupees(-250)),
            viewModel.uiState.value.summary
        )
    }

    @Test
    fun search_waitsForTypingToPause() = runTest {
        val viewModel = viewModel(searchable)
        subscribe(viewModel.uiState)

        viewModel.search("p")
        advanceTimeBy(200)
        viewModel.search("pa")
        advanceTimeBy(200)
        runCurrent()

        // 400ms since the first letter, but only 200ms since the last
        assertEquals(listOf(pay, teamLunch), viewModel.uiState.value.transactions)

        advanceTimeBy(101)
        runCurrent()

        // "pa" is in "October pay" and in the note "Paid for Riya"
        assertEquals(listOf(pay, teamLunch), viewModel.uiState.value.transactions)

        viewModel.search("pay")
        advanceTimeBy(301)
        runCurrent()

        assertEquals(listOf(pay), viewModel.uiState.value.transactions)
    }

    @Test
    fun search_matchesTitleNoteAndCategory_ignoringCase() = runTest {
        val viewModel = viewModel(searchable)
        subscribe(viewModel.uiState)

        mapOf("TEAM" to teamLunch, "riya" to teamLunch, "salary" to pay).forEach { (query, expected) ->
            viewModel.search(query)
            advanceTimeBy(301)
            runCurrent()

            assertEquals(query, listOf(expected), viewModel.uiState.value.transactions)
        }
    }

    @Test
    fun clearingTheSearch_bringsEverythingBackWithoutWaiting() = runTest {
        val viewModel = viewModel(searchable)
        subscribe(viewModel.uiState)
        viewModel.search("lunch")
        advanceTimeBy(301)
        runCurrent()

        viewModel.search("")
        runCurrent()

        assertEquals(listOf(pay, teamLunch), viewModel.uiState.value.transactions)
    }

    @Test
    fun search_staysWithinThePeriodAndCategoryFilter() = runTest {
        val viewModel = viewModel(searchable)
        subscribe(viewModel.uiState)

        // oldRent is in September
        viewModel.search(oldRent.title)
        advanceTimeBy(301)
        runCurrent()

        assertTrue(viewModel.uiState.value.transactions.isEmpty())
    }

    @Test
    fun searchOpened_thenClosed_clearsTheQueryAndItsResults() = runTest {
        val viewModel = viewModel(searchable)
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.SearchOpened)
        assertTrue(viewModel.uiState.value.isSearchOpen)

        viewModel.search("lunch")
        advanceTimeBy(301)
        runCurrent()
        assertTrue(viewModel.uiState.value.isShowingSearchResults)

        viewModel.onEvent(TransactionListEvent.SearchClosed)
        runCurrent()
        val closed = viewModel.uiState.value

        assertFalse(closed.isSearchOpen)
        assertEquals("", closed.searchQuery)
        assertFalse(closed.isShowingSearchResults)
        assertEquals(listOf(pay, teamLunch), closed.transactions)
    }

    @Test
    fun searchAllTime_findsWhatThePeriodHid_andKeepsTheQuery() = runTest {
        val viewModel = viewModel(searchable)
        subscribe(viewModel.uiState)
        viewModel.search(oldRent.title)
        advanceTimeBy(301)
        runCurrent()
        assertTrue(viewModel.uiState.value.transactions.isEmpty())
        assertFalse(viewModel.uiState.value.isAllTime)

        viewModel.onEvent(TransactionListEvent.SearchAllTime)
        runCurrent()
        val state = viewModel.uiState.value

        assertTrue(state.isAllTime)
        assertEquals(
            PeriodFilterState(
                filter = DateFilter.Custom(DateRange.ALL),
                dateRange = DateRange.ALL,
                canStepForward = false
            ),
            state.filter
        )
        assertEquals(oldRent.title, state.searchQuery)
        assertEquals(listOf(oldRent), state.transactions)
    }

    @Test
    fun allTime_goesBackToThisMonth_whenAGranularityIsChosen() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(TransactionListEvent.SearchAllTime)
        assertEquals(listOf(salary, lunch, oldRent), viewModel.uiState.value.transactions)

        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.MONTH))

        assertEquals(thisMonth, viewModel.uiState.value.filter)
    }
}
