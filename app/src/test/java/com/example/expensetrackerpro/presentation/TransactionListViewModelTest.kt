package com.example.expensetrackerpro.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateSavingsRateUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ObserveDeletedTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.RestoreTransactionUseCase
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private val salary = transaction(id = 1, amount = 1000.0, type = TransactionType.INCOME, dateMillis = now)
    private val lunch = transaction(
        id = 2,
        amount = 250.0,
        type = TransactionType.EXPENSE,
        dateMillis = millisAt(2026, Calendar.OCTOBER, 2)
    )
    private val oldRent = transaction(
        id = 3,
        amount = 400.0,
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
            calculateFinancialSummary = CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase()),
            deleteTransaction = DeleteTransactionUseCase(repository),
            observeDeletedTransactions = ObserveDeletedTransactionsUseCase(repository),
            restoreTransaction = RestoreTransactionUseCase(repository),
            savedStateHandle = savedStateHandle
        )

    private fun TransactionListViewModel.onFilterEvent(event: PeriodFilterEvent) =
        onEvent(TransactionListEvent.Filter(event))

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
        assertEquals(1000.0, state.totalIncome, 0.001)
        assertEquals(250.0, state.totalExpense, 0.001)
        assertEquals(750.0, state.totalBalance, 0.001)
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
        assertEquals(-400.0, september.totalBalance, 0.001)

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
        assertEquals(350.0, viewModel.uiState.value.totalBalance, 0.001)
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
        assertEquals(650.0, state.totalExpense, 0.001)

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
                    Screens.History.ARG_START to secondOfOctober.startMillis,
                    Screens.History.ARG_END to secondOfOctober.endMillis
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
                    Screens.History.ARG_START to millisAt(2026, Calendar.SEPTEMBER, 1),
                    Screens.History.ARG_GRANULARITY to PeriodGranularity.MONTH.name
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
        val travel = lunch.copy(id = 4, categoryId = 7, categoryName = "Travel", amount = 90.0)
        val repository = FakeTransactionRepository(listOf(salary, lunch, oldRent, travel))
        val viewModel = viewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screens.History.ARG_START to october.startMillis,
                    Screens.History.ARG_GRANULARITY to PeriodGranularity.MONTH.name,
                    Screens.History.ARG_CATEGORY_ID to 7L,
                    Screens.History.ARG_CATEGORY_NAME to "Travel"
                )
            )
        )
        subscribe(viewModel.uiState)
        val filtered = viewModel.uiState.value

        assertEquals(thisMonth, filtered.filter)
        assertEquals(CategoryFilter(id = 7, name = "Travel"), filtered.categoryFilter)
        assertEquals(listOf(travel), filtered.transactions)
        // Totals follow what is listed
        assertEquals(90.0, filtered.totalExpense, 0.001)
        assertEquals(0.0, filtered.totalIncome, 0.001)

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
                    Screens.History.ARG_START to october.startMillis,
                    Screens.History.ARG_GRANULARITY to PeriodGranularity.MONTH.name,
                    Screens.History.ARG_CATEGORY_ID to 1L,
                    Screens.History.ARG_CATEGORY_NAME to "Food"
                )
            )
        )
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)

        assertEquals(CategoryFilter(id = 1, name = "Food"), viewModel.uiState.value.categoryFilter)
        assertEquals(listOf(oldRent), viewModel.uiState.value.transactions)
    }

    @Test
    fun openedWithTheRoutesDefaults_isThePlainHistory() = runTest {
        val viewModel = viewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    Screens.History.ARG_START to Screens.History.NO_VALUE,
                    Screens.History.ARG_END to Screens.History.NO_VALUE,
                    Screens.History.ARG_CATEGORY_ID to Screens.History.NO_VALUE
                )
            )
        )
        subscribe(viewModel.uiState)

        assertEquals(thisMonth, viewModel.uiState.value.filter)
        assertNull(viewModel.uiState.value.categoryFilter)
        assertEquals(listOf(salary, lunch), viewModel.uiState.value.transactions)
    }

    @Test
    fun deleteRequested_thenConfirmed_removesAndClosesDialog() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.DeleteRequested(lunch))
        assertEquals(lunch, viewModel.uiState.value.transactionToDelete)

        viewModel.onEvent(TransactionListEvent.DeleteConfirmed)
        val state = viewModel.uiState.value

        assertNull(state.transactionToDelete)
        assertEquals(listOf(salary), state.transactions)
        assertEquals(0.0, state.totalExpense, 0.001)
        assertEquals(listOf(salary, oldRent), repository.current.sortedBy { it.id })
    }

    @Test
    fun deleteDismissed_keepsTransaction() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.DeleteRequested(lunch))
        viewModel.onEvent(TransactionListEvent.DeleteDismissed)
        val state = viewModel.uiState.value

        assertNull(state.transactionToDelete)
        assertEquals(listOf(salary, lunch), state.transactions)
    }

    @Test
    fun deleteConfirmed_withoutRequest_doesNothing() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.DeleteConfirmed)

        assertEquals(3, repository.current.size)
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
        assertEquals(250.0, viewModel.uiState.value.totalExpense, 0.001)
        assertEquals(0.0, viewModel.uiState.value.totalIncome, 0.001)
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

    // --- Undo

    private fun TestScope.effectsOf(viewModel: TransactionListViewModel): List<TransactionListEffect> {
        val effects = mutableListOf<TransactionListEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.toList(effects)
        }
        return effects
    }

    @Test
    fun deleteConfirmed_offersUndo_andUndoRestoresTheTransaction() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val effects = effectsOf(viewModel)

        viewModel.onEvent(TransactionListEvent.DeleteRequested(lunch))
        viewModel.onEvent(TransactionListEvent.DeleteConfirmed)

        assertEquals(listOf<TransactionListEffect>(TransactionListEffect.ShowUndoDelete(lunch)), effects)
        assertEquals(listOf(salary), viewModel.uiState.value.transactions)

        viewModel.onEvent(TransactionListEvent.UndoDelete(lunch))

        assertEquals(listOf(salary, lunch), viewModel.uiState.value.transactions)
        assertEquals(250.0, viewModel.uiState.value.totalExpense, 0.001)
    }

    @Test
    fun deleteMadeElsewhere_alsoOffersUndo() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        // As the edit screen does, while History sits behind it
        repository.deleteTransaction(lunch)

        assertEquals(listOf<TransactionListEffect>(TransactionListEffect.ShowUndoDelete(lunch)), effects)
    }

    @Test
    fun deleteMadeBeforeHistoryExists_offersNothing() = runTest {
        repository.deleteTransaction(lunch)

        assertTrue(effectsOf(viewModel()).isEmpty())
    }

    @Test
    fun failedUndo_isReported() = runTest {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        repository.deleteTransaction(lunch)
        repository.failWrites = true

        viewModel.onEvent(TransactionListEvent.UndoDelete(lunch))

        assertEquals(TransactionListEffect.ShowRestoreFailed, effects.last())
        assertEquals(2, repository.current.size)
    }
}
