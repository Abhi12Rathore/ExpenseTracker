package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.getDashboardUseCase
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeEvent
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeUiState
import com.example.expensetrackerpro.presentation.homeScreen.viewmodels.HomeViewModel
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** What each section of the dashboard holds is covered by GetDashboardUseCaseTest. */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactions = FakeTransactionRepository()
    private val dashboard = FakeDashboardRepository()

    // The dashboard is opened on Thursday 8 Oct 2026
    private val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 8, hour = 12))
    private val october =
        DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1))
    private val september =
        DateRange(millisAt(2026, Calendar.SEPTEMBER, 1), millisAt(2026, Calendar.OCTOBER, 1))

    private val thisMonth = PeriodFilterState(
        filter = DateFilter.Period(PeriodGranularity.MONTH, october.startMillis),
        dateRange = october,
        canStepForward = false
    )
    private val lastMonth = PeriodFilterState(
        filter = DateFilter.Period(PeriodGranularity.MONTH, september.startMillis),
        dateRange = september,
        canStepForward = true
    )

    private fun viewModel() = HomeViewModel(
        getDashboard = getDashboardUseCase(transactions, dashboard, timeProvider),
        getDateRange = GetDateRangeUseCase(timeProvider),
        getCurrentPeriod = GetCurrentPeriodUseCase(timeProvider),
        stepPeriod = StepPeriodUseCase(timeProvider),
        getCustomDateRange = GetCustomDateRangeUseCase(timeProvider)
    )

    private fun HomeViewModel.onFilterEvent(event: PeriodFilterEvent) =
        onEvent(HomeEvent.Filter(event))

    /** A September salary and rent, then an October salary and lunch. */
    private suspend fun saveTwoMonths() {
        transactions.saveTransaction(
            transaction(id = 1, amount = rupees(900), type = TransactionType.INCOME, dateMillis = millisAt(2026, Calendar.SEPTEMBER, 5))
        )
        transactions.saveTransaction(
            transaction(id = 2, amount = rupees(400), type = TransactionType.EXPENSE, dateMillis = millisAt(2026, Calendar.SEPTEMBER, 30, hour = 23))
        )
        transactions.saveTransaction(
            transaction(id = 3, amount = rupees(1000), type = TransactionType.INCOME, dateMillis = millisAt(2026, Calendar.OCTOBER, 1))
        )
        transactions.saveTransaction(
            transaction(id = 4, amount = rupees(250), type = TransactionType.EXPENSE, dateMillis = millisAt(2026, Calendar.OCTOBER, 7))
        )
    }

    // --- Loading ---

    @Test
    fun beforeAnyoneSubscribes_alreadyShowsThisMonth_asLoading() {
        assertEquals(HomeUiState(filter = thisMonth, isLoading = true), viewModel().uiState.value)
    }

    @Test
    fun onceDataArrives_loadingEnds_andTheGreetingIsSet() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(thisMonth, state.filter)
        assertEquals(DayPeriod.AFTERNOON, state.dashboard.dayPeriod)
    }

    @Test
    fun byDefault_theDashboardIsForThisMonth() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertEquals(thisMonth, state.filter)
        assertEquals(rupees(1000), state.dashboard.summary.totalIncome)
        assertEquals(listOf(4, 3), state.dashboard.recentTransactions.map { it.id })
        assertEquals(31, state.dashboard.spending.buckets.size)
    }

    // --- Stepping ---

    @Test
    fun previousPeriod_movesTheDashboardBackOneMonth() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        val state = viewModel.uiState.value

        assertEquals(lastMonth, state.filter)
        assertEquals(rupees(900), state.dashboard.summary.totalIncome)
        assertEquals(listOf(2, 1), state.dashboard.recentTransactions.map { it.id })
        assertEquals(30, state.dashboard.spending.buckets.size)
    }

    @Test
    fun nextPeriod_returnsToThisMonth_thenStopsAtTheFuture() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        viewModel.onFilterEvent(PeriodFilterEvent.NextPeriod)
        assertEquals(thisMonth, viewModel.uiState.value.filter)

        // November hasn't started
        viewModel.onFilterEvent(PeriodFilterEvent.NextPeriod)
        assertEquals(thisMonth, viewModel.uiState.value.filter)
        assertEquals(listOf(4, 3), viewModel.uiState.value.dashboard.recentTransactions.map { it.id })
    }

    @Test
    fun steppingBackFromJanuary_crossesIntoThePreviousYear() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        // October back to January, then one more
        repeat(10) { viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod) }

        assertEquals(
            DateRange(millisAt(2025, Calendar.DECEMBER, 1), millisAt(2026, Calendar.JANUARY, 1)),
            viewModel.uiState.value.filter.dateRange
        )
    }

    // --- Granularity ---

    @Test
    fun granularitySelected_showsTheCurrentWeekOrYear() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.YEAR))
        val year = viewModel.uiState.value
        val year2026 =
            DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2027, Calendar.JANUARY, 1))

        assertEquals(
            PeriodFilterState(
                filter = DateFilter.Period(PeriodGranularity.YEAR, year2026.startMillis),
                dateRange = year2026,
                canStepForward = false
            ),
            year.filter
        )
        assertEquals(rupees(1250), year.dashboard.summary.totalBalance)
        assertEquals(BucketSize.MONTH, year.dashboard.spending.bucketSize)
        assertEquals(12, year.dashboard.netSavings.size)
        assertNull(year.dashboard.pace)

        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.WEEK))
        val week = viewModel.uiState.value

        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 11)),
            week.filter.dateRange
        )
        assertEquals(listOf(4), week.dashboard.recentTransactions.map { it.id })
        assertTrue(week.dashboard.netSavings.isEmpty())
        assertEquals(PeriodGranularity.WEEK, week.dashboard.pace?.granularity)
    }

    @Test
    fun granularitySelected_afterSteppingBack_jumpsToTheCurrentPeriod() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        repeat(3) { viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod) }
        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.MONTH))

        assertEquals(thisMonth, viewModel.uiState.value.filter)
    }

    // --- Picking a month from the year's chart ---

    @Test
    fun monthSelected_narrowsTheYearToThatMonth() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.YEAR))

        viewModel.onEvent(HomeEvent.MonthSelected(september.startMillis))
        val state = viewModel.uiState.value

        assertEquals(lastMonth, state.filter)
        assertEquals(listOf(2, 1), state.dashboard.recentTransactions.map { it.id })
        assertTrue(state.dashboard.netSavings.isEmpty())
    }

    @Test
    fun monthSelected_theCurrentMonth_cannotStepForward_andArrowsWorkFromThere() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.YEAR))

        viewModel.onEvent(HomeEvent.MonthSelected(october.startMillis))
        assertEquals(thisMonth, viewModel.uiState.value.filter)

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        assertEquals(lastMonth, viewModel.uiState.value.filter)
    }

    // --- Custom range ---

    @Test
    fun customRangeSelected_coversThePickedDaysInclusive() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val utc = TimeZone.getTimeZone("UTC")

        // 30 Sep to 1 Oct, as a date picker reports them
        viewModel.onFilterEvent(
            PeriodFilterEvent.CustomRangeSelected(
                startUtcMillis = millisAt(2026, Calendar.SEPTEMBER, 30, timeZone = utc),
                endUtcMillis = millisAt(2026, Calendar.OCTOBER, 1, timeZone = utc)
            )
        )
        val state = viewModel.uiState.value
        val picked =
            DateRange(millisAt(2026, Calendar.SEPTEMBER, 30), millisAt(2026, Calendar.OCTOBER, 2))

        assertEquals(
            PeriodFilterState(filter = DateFilter.Custom(picked), dateRange = picked, canStepForward = false),
            state.filter
        )
        assertEquals(listOf(3, 2), state.dashboard.recentTransactions.map { it.id })
        assertEquals(rupees(600), state.dashboard.summary.totalBalance)
        assertNull(state.dashboard.comparison)
    }

    @Test
    fun customRange_ignoresTheArrows_andASegmentLeavesIt() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val utc = TimeZone.getTimeZone("UTC")
        viewModel.onFilterEvent(
            PeriodFilterEvent.CustomRangeSelected(
                startUtcMillis = millisAt(2026, Calendar.JUNE, 1, timeZone = utc),
                endUtcMillis = millisAt(2026, Calendar.JUNE, 10, timeZone = utc)
            )
        )
        val custom = viewModel.uiState.value.filter

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        viewModel.onFilterEvent(PeriodFilterEvent.NextPeriod)
        assertEquals(custom, viewModel.uiState.value.filter)

        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.MONTH))
        assertEquals(thisMonth, viewModel.uiState.value.filter)
    }
}
