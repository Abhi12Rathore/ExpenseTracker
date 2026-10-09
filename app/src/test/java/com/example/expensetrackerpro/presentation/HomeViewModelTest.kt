package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryRanking
import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateSavingsRateUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GenerateSmartInsightsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetBalanceTrendUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCashFlowUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCategoryBreakdownUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetDayPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetMonthlyTrendUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetPeriodComparisonUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetRecentTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetSpendingBucketsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.RankCategorySpendingUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeEvent
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeUiState
import com.example.expensetrackerpro.presentation.homeScreen.viewmodels.HomeViewModel
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import com.example.expensetrackerpro.utils.DateFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

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

    private val getFinancialSummary = GetFinancialSummaryUseCase(
        transactions,
        CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase())
    )

    private fun viewModel() = HomeViewModel(
        getFinancialSummary = getFinancialSummary,
        getRecentTransactions = GetRecentTransactionsUseCase(transactions),
        getCategoryBreakdown = GetCategoryBreakdownUseCase(dashboard),
        getSpendingBuckets = GetSpendingBucketsUseCase(dashboard, timeProvider),
        getBalanceTrend = GetBalanceTrendUseCase(GetCashFlowUseCase(dashboard), timeProvider),
        getPeriodComparison = GetPeriodComparisonUseCase(
            getFinancialSummary,
            GetDateRangeUseCase(timeProvider),
            StepPeriodUseCase(timeProvider)
        ),
        rankCategorySpending = RankCategorySpendingUseCase(),
        getMonthlyTrend = GetMonthlyTrendUseCase(dashboard),
        generateSmartInsights = GenerateSmartInsightsUseCase(),
        getDayPeriod = GetDayPeriodUseCase(),
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
            transaction(id = 1, amount = 900.0, type = TransactionType.INCOME, dateMillis = millisAt(2026, Calendar.SEPTEMBER, 5))
        )
        transactions.saveTransaction(
            transaction(id = 2, amount = 400.0, type = TransactionType.EXPENSE, dateMillis = millisAt(2026, Calendar.SEPTEMBER, 30, hour = 23))
        )
        transactions.saveTransaction(
            transaction(id = 3, amount = 1000.0, type = TransactionType.INCOME, dateMillis = millisAt(2026, Calendar.OCTOBER, 1))
        )
        transactions.saveTransaction(
            transaction(id = 4, amount = 250.0, type = TransactionType.EXPENSE, dateMillis = millisAt(2026, Calendar.OCTOBER, 7))
        )
    }

    private fun assertChartsAskedFor(range: DateRange) {
        assertEquals(range, dashboard.categoryBreakdownRange)
        assertEquals(range, dashboard.cashFlowRange)
        assertEquals(range, dashboard.dailyExpensesRange)
        assertEquals(range, dashboard.monthlyTrendRange)
    }

    @Test
    fun noData_isEmptyStateForThisMonthWithGreeting() = runTest {
        val before = System.currentTimeMillis()
        val viewModel = viewModel()
        val after = System.currentTimeMillis()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertEquals(thisMonth, state.filter)
        assertFalse(state.isLoading)
        assertEquals(FinancialSummary(), state.financialSummary)
        assertEquals(CategoryRanking(), state.categoryRanking)
        assertFalse(state.spending.hasSpending)
        assertTrue(state.monthlyTrend.isEmpty())
        assertTrue(state.recentTransactions.isEmpty())
        assertTrue(state.insights.isEmpty())
        // The greeting uses the real clock, which may tick into the next period mid-test
        val possiblePeriods = listOf(before, after).map { GetDayPeriodUseCase()(DateFormatter.hourOfDay(it)) }
        assertTrue(state.dayPeriod in possiblePeriods)
    }

    @Test
    fun beforeAnyoneSubscribes_alreadyShowsThisMonth_asLoading() {
        val state = viewModel().uiState.value

        assertEquals(HomeUiState(dayPeriod = state.dayPeriod, filter = thisMonth, isLoading = true), state)
    }

    @Test
    fun exposesSummaryRecentAndCharts() = runTest {
        (1..7).forEach {
            transactions.saveTransaction(
                transaction(id = it, amount = 100.0, type = TransactionType.EXPENSE, dateMillis = october.startMillis + it)
            )
        }
        transactions.saveTransaction(
            transaction(id = 8, amount = 1400.0, type = TransactionType.INCOME, dateMillis = october.startMillis + 8)
        )
        val breakdown = listOf(CategoryBreakdown(categoryId = 1, categoryName = "Food", iconName = "Food", amount = 700.0))
        dashboard.categoryBreakdown.value = breakdown

        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertEquals(1400.0, state.financialSummary.totalIncome, 0.001)
        assertEquals(700.0, state.financialSummary.totalExpense, 0.001)
        assertEquals(50f, state.financialSummary.savingsRate, 0.001f)
        assertEquals(listOf(8, 7, 6, 5, 4), state.recentTransactions.map { it.id })
        assertEquals(breakdown, state.categoryRanking.top.map { it.category })
        assertEquals(700.0, state.categoryRanking.total, 0.001)
    }

    @Test
    fun spending_isOneBucketPerDayOfTheMonth_withTodayTheLastStarted() = runTest {
        dashboard.dailyExpenses.value = listOf(
            DailyExpense(millisAt(2026, Calendar.OCTOBER, 2, hour = 9), amount = 300.0, transactionCount = 2),
            DailyExpense(millisAt(2026, Calendar.OCTOBER, 7, hour = 20), amount = 500.0, transactionCount = 1)
        )
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val spending = viewModel.uiState.value.spending

        assertEquals(BucketSize.DAY, spending.bucketSize)
        assertEquals(31, spending.buckets.size)
        assertEquals(300.0, spending.buckets[1].amount, 0.001)
        assertEquals(2, spending.buckets[1].transactionCount)
        assertEquals(8, spending.buckets.count { !it.isFuture })
        assertEquals(100.0, spending.average, 0.001)
    }

    @Test
    fun yearView_bucketsSpendingByMonth() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.YEAR))
        val spending = viewModel.uiState.value.spending

        assertEquals(BucketSize.MONTH, spending.bucketSize)
        assertEquals(12, spending.buckets.size)
    }

    @Test
    fun comparison_isAgainstThePreviousPeriod_andFollowsTheFilter() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        // October saved 750, September 500
        assertEquals(
            PeriodComparison(PeriodGranularity.MONTH, september, balanceDifference = 250.0),
            viewModel.uiState.value.periodComparison
        )

        // September against an empty August
        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        assertEquals(500.0, viewModel.uiState.value.periodComparison?.balanceDifference ?: 0.0, 0.001)
    }

    @Test
    fun customRange_hasNoComparison() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val utc = TimeZone.getTimeZone("UTC")

        viewModel.onFilterEvent(
            PeriodFilterEvent.CustomRangeSelected(
                startUtcMillis = millisAt(2026, Calendar.SEPTEMBER, 30, timeZone = utc),
                endUtcMillis = millisAt(2026, Calendar.OCTOBER, 1, timeZone = utc)
            )
        )

        assertNull(viewModel.uiState.value.periodComparison)
        assertEquals(BucketSize.DAY, viewModel.uiState.value.spending.bucketSize)
        assertEquals(2, viewModel.uiState.value.spending.buckets.size)
    }

    @Test
    fun balanceTrend_runsFromTheStartOfThePeriodToToday() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals(List(8) { 0.0 }, viewModel.uiState.value.balanceTrend)

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        assertEquals(30, viewModel.uiState.value.balanceTrend.size)
    }

    @Test
    fun byDefault_everySectionCoversThisMonthOnly() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertEquals(1000.0, state.financialSummary.totalIncome, 0.001)
        assertEquals(250.0, state.financialSummary.totalExpense, 0.001)
        assertEquals(listOf(4, 3), state.recentTransactions.map { it.id })
        assertEquals(listOf(SmartInsight.Saved(750.0)), state.insights)
        assertChartsAskedFor(october)
    }

    @Test
    fun previousPeriod_movesEverySectionBackOneMonth() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        val state = viewModel.uiState.value

        assertEquals(lastMonth, state.filter)
        assertEquals(900.0, state.financialSummary.totalIncome, 0.001)
        assertEquals(400.0, state.financialSummary.totalExpense, 0.001)
        assertEquals(listOf(2, 1), state.recentTransactions.map { it.id })
        assertEquals(listOf(SmartInsight.Saved(500.0)), state.insights)
        assertChartsAskedFor(september)
    }

    @Test
    fun nextPeriod_returnsToThisMonth_thenStopsAtTheFuture() = runTest {
        saveTwoMonths()
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod)
        viewModel.onFilterEvent(PeriodFilterEvent.NextPeriod)
        assertEquals(thisMonth, viewModel.uiState.value.filter)
        assertChartsAskedFor(october)

        // November hasn't started
        viewModel.onFilterEvent(PeriodFilterEvent.NextPeriod)
        assertEquals(thisMonth, viewModel.uiState.value.filter)
        assertEquals(listOf(4, 3), viewModel.uiState.value.recentTransactions.map { it.id })
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
        assertEquals(1250.0, year.financialSummary.totalBalance, 0.001)
        assertEquals(listOf(4, 3, 2, 1), year.recentTransactions.map { it.id })
        assertChartsAskedFor(year2026)

        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.WEEK))
        val week = viewModel.uiState.value
        val thisWeek =
            DateRange(millisAt(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 11))

        assertEquals(thisWeek, week.filter.dateRange)
        assertEquals(listOf(4), week.recentTransactions.map { it.id })
        assertChartsAskedFor(thisWeek)
    }

    @Test
    fun granularitySelected_afterSteppingBack_jumpsToTheCurrentPeriod() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        repeat(3) { viewModel.onFilterEvent(PeriodFilterEvent.PreviousPeriod) }
        viewModel.onFilterEvent(PeriodFilterEvent.GranularitySelected(PeriodGranularity.MONTH))

        assertEquals(thisMonth, viewModel.uiState.value.filter)
    }

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
        assertEquals(listOf(3, 2), state.recentTransactions.map { it.id })
        assertEquals(600.0, state.financialSummary.totalBalance, 0.001)
        assertChartsAskedFor(picked)
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

    @Test
    fun insightsFollowTheData() = runTest {
        transactions.saveTransaction(
            transaction(id = 1, amount = 500.0, type = TransactionType.INCOME, dateMillis = october.startMillis)
        )
        dashboard.categoryBreakdown.value =
            listOf(CategoryBreakdown(categoryId = 1, categoryName = "Bills", iconName = "Bills", amount = 90.0))
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals(
            listOf(SmartInsight.Saved(500.0), SmartInsight.HighestSpending("Bills")),
            viewModel.uiState.value.insights
        )

        dashboard.monthlyTrend.value = listOf(
            MonthlyTrend(year = 2026, month = 6, income = 0.0, expense = 100.0),
            MonthlyTrend(year = 2026, month = 7, income = 0.0, expense = 300.0)
        )

        assertEquals(
            listOf(
                SmartInsight.Saved(500.0),
                SmartInsight.HighestSpending("Bills"),
                SmartInsight.SpendingIncreased
            ),
            viewModel.uiState.value.insights
        )
    }
}
