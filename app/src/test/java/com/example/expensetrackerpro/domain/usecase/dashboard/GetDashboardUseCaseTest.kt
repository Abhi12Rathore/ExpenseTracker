package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryRanking
import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.Dashboard
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.getDashboardUseCase
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GetDashboardUseCaseTest {

    private val transactions = FakeTransactionRepository()
    private val dashboard = FakeDashboardRepository()

    // Thursday 8 Oct 2026, just after noon
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)

    private val october =
        DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1))
    private val september =
        DateRange(millisAt(2026, Calendar.SEPTEMBER, 1), millisAt(2026, Calendar.OCTOBER, 1))
    private val year2026 =
        DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2027, Calendar.JANUARY, 1))

    private val thisMonth = DateFilter.Period(PeriodGranularity.MONTH, october.startMillis)
    private val lastMonth = DateFilter.Period(PeriodGranularity.MONTH, september.startMillis)
    private val thisWeek = DateFilter.Period(PeriodGranularity.WEEK, millisAt(2026, Calendar.OCTOBER, 4))
    private val thisYear = DateFilter.Period(PeriodGranularity.YEAR, year2026.startMillis)
    // 30 Sep and 1 Oct
    private val twoDays = DateFilter.Custom(
        DateRange(millisAt(2026, Calendar.SEPTEMBER, 30), millisAt(2026, Calendar.OCTOBER, 2))
    )

    private fun getDashboard(nowMillis: Long = now) =
        getDashboardUseCase(transactions, dashboard, FakeTimeProvider(nowMillis))

    private suspend fun dashboardFor(filter: DateFilter): Dashboard = getDashboard()(filter).first()

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
        assertEquals(range, dashboard.monthlyTrendRange)
    }

    // --- Empty and greeting ---

    @Test
    fun noData_isAnEmptyDashboard() = runTest {
        val state = dashboardFor(thisMonth)

        assertEquals(FinancialSummary(), state.summary)
        assertNull(state.comparison)
        assertEquals(CategoryRanking(), state.categoryRanking)
        assertFalse(state.spending.hasSpending)
        assertFalse(requireNotNull(state.pace).hasSpending)
        assertTrue(state.netSavings.isEmpty())
        assertTrue(state.recentTransactions.isEmpty())
        assertTrue(state.insights.isEmpty())
    }

    @Test
    fun greeting_followsTheProvidersClock() = runTest {
        suspend fun dayPeriodAt(hour: Int) =
            getDashboard(millisAt(2026, Calendar.OCTOBER, 8, hour = hour))(thisMonth).first().dayPeriod

        assertEquals(DayPeriod.MORNING, dayPeriodAt(8))
        assertEquals(DayPeriod.AFTERNOON, dayPeriodAt(12))
        assertEquals(DayPeriod.EVENING, dayPeriodAt(20))
        assertEquals(DayPeriod.EVENING, dayPeriodAt(2))
    }

    // --- Sections ---

    @Test
    fun exposesSummaryRecentAndCategories() = runTest {
        (1..7).forEach {
            transactions.saveTransaction(
                transaction(id = it, amount = 100.0, type = TransactionType.EXPENSE, dateMillis = october.startMillis + it)
            )
        }
        transactions.saveTransaction(
            transaction(id = 8, amount = 1400.0, type = TransactionType.INCOME, dateMillis = october.startMillis + 8)
        )
        val breakdown =
            listOf(CategoryBreakdown(categoryId = 1, categoryName = "Food", iconName = "Food", amount = 700.0))
        dashboard.categoryBreakdown.value = breakdown

        val state = dashboardFor(thisMonth)

        assertEquals(1400.0, state.summary.totalIncome, 0.001)
        assertEquals(700.0, state.summary.totalExpense, 0.001)
        assertEquals(50f, state.summary.savingsRate, 0.001f)
        assertEquals(listOf(8, 7, 6, 5, 4), state.recentTransactions.map { it.id })
        assertEquals(breakdown, state.categoryRanking.top.map { it.category })
        assertEquals(700.0, state.categoryRanking.total, 0.001)
    }

    @Test
    fun month_coversThatMonthOnly_inEverySection() = runTest {
        saveTwoMonths()

        val thisMonthState = dashboardFor(thisMonth)

        assertEquals(1000.0, thisMonthState.summary.totalIncome, 0.001)
        assertEquals(250.0, thisMonthState.summary.totalExpense, 0.001)
        assertEquals(listOf(4, 3), thisMonthState.recentTransactions.map { it.id })
        assertEquals(listOf(SmartInsight.Saved(750.0)), thisMonthState.insights)
        assertChartsAskedFor(october)

        val lastMonthState = dashboardFor(lastMonth)

        assertEquals(900.0, lastMonthState.summary.totalIncome, 0.001)
        assertEquals(400.0, lastMonthState.summary.totalExpense, 0.001)
        assertEquals(listOf(2, 1), lastMonthState.recentTransactions.map { it.id })
        assertEquals(listOf(SmartInsight.Saved(500.0)), lastMonthState.insights)
        assertChartsAskedFor(september)
    }

    @Test
    fun week_coversThatWeekOnly() = runTest {
        saveTwoMonths()

        val state = dashboardFor(thisWeek)

        assertEquals(listOf(4), state.recentTransactions.map { it.id })
        assertChartsAskedFor(DateRange(millisAt(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 11)))
    }

    @Test
    fun year_coversTheWholeYear() = runTest {
        saveTwoMonths()

        val state = dashboardFor(thisYear)

        assertEquals(1250.0, state.summary.totalBalance, 0.001)
        assertEquals(listOf(4, 3, 2, 1), state.recentTransactions.map { it.id })
        assertChartsAskedFor(year2026)
    }

    @Test
    fun customRange_coversThePickedDays() = runTest {
        saveTwoMonths()

        val state = dashboardFor(twoDays)

        assertEquals(listOf(3, 2), state.recentTransactions.map { it.id })
        assertEquals(600.0, state.summary.totalBalance, 0.001)
        assertChartsAskedFor(twoDays.range)
    }

    // --- Spending buckets and balance trend ---

    @Test
    fun spending_isOneBucketPerDayOfTheMonth_withTodayTheLastStarted() = runTest {
        dashboard.dailyExpenses.value = listOf(
            DailyExpense(millisAt(2026, Calendar.OCTOBER, 2, hour = 9), amount = 300.0, transactionCount = 2),
            DailyExpense(millisAt(2026, Calendar.OCTOBER, 7, hour = 20), amount = 500.0, transactionCount = 1)
        )

        val spending = dashboardFor(thisMonth).spending

        assertEquals(BucketSize.DAY, spending.bucketSize)
        assertEquals(31, spending.buckets.size)
        assertEquals(300.0, spending.buckets[1].amount, 0.001)
        assertEquals(2, spending.buckets[1].transactionCount)
        assertEquals(8, spending.buckets.count { !it.isFuture })
        assertEquals(100.0, spending.average, 0.001)
    }

    @Test
    fun spending_isBucketedByMonthForAYear_andByDayForACustomRange() = runTest {
        val year = dashboardFor(thisYear).spending
        assertEquals(BucketSize.MONTH, year.bucketSize)
        assertEquals(12, year.buckets.size)

        val custom = dashboardFor(twoDays).spending
        assertEquals(BucketSize.DAY, custom.bucketSize)
        assertEquals(2, custom.buckets.size)
    }

    @Test
    fun balanceTrend_runsFromTheStartOfThePeriodToToday() = runTest {
        assertEquals(List(8) { 0.0 }, dashboardFor(thisMonth).balanceTrend)
        assertEquals(30, dashboardFor(lastMonth).balanceTrend.size)
    }

    // --- Comparison ---

    @Test
    fun comparison_ofAnOngoingMonth_isAgainstTheSameDaysOfTheMonthBefore() = runTest {
        saveTwoMonths()

        // October has saved 750. By 8 Sep the salary was in but the rent wasn't out: 900
        assertEquals(
            PeriodComparison(
                granularity = PeriodGranularity.MONTH,
                previousRange = DateRange(september.startMillis, millisAt(2026, Calendar.SEPTEMBER, 9)),
                balanceDifference = -150.0,
                isPartial = true
            ),
            dashboardFor(thisMonth).comparison
        )
    }

    @Test
    fun comparison_ofAFinishedMonth_isAgainstTheWholeMonthBefore() = runTest {
        saveTwoMonths()

        // September saved 500 against an empty August
        val comparison = requireNotNull(dashboardFor(lastMonth).comparison)

        assertEquals(500.0, comparison.balanceDifference, 0.001)
        assertFalse(comparison.isPartial)
    }

    @Test
    fun customRange_hasNoComparisonPaceOrMonthlyBreakdown() = runTest {
        saveTwoMonths()

        val state = dashboardFor(twoDays)

        assertNull(state.comparison)
        assertNull(state.pace)
        assertTrue(state.netSavings.isEmpty())
    }

    // --- Pace and net savings ---

    @Test
    fun pace_isThereForAMonthAndAWeek_butNotForAYear() = runTest {
        dashboard.dailyExpenses.value = listOf(
            DailyExpense(millisAt(2026, Calendar.SEPTEMBER, 3, hour = 9), amount = 200.0, transactionCount = 1),
            DailyExpense(millisAt(2026, Calendar.SEPTEMBER, 20, hour = 9), amount = 300.0, transactionCount = 1),
            DailyExpense(millisAt(2026, Calendar.OCTOBER, 2, hour = 9), amount = 150.0, transactionCount = 1)
        )

        val month = requireNotNull(dashboardFor(thisMonth).pace)
        assertEquals(8, month.current.size)
        assertEquals(150.0, month.current.last().total, 0.001)
        assertEquals(30, month.previous.size)
        assertEquals(500.0, month.previous.last().total, 0.001)
        // 150 so far against the 200 spent by 8 Sep
        assertEquals(-50.0, month.difference, 0.001)
        assertTrue(month.isPartial)

        assertEquals(PeriodGranularity.WEEK, requireNotNull(dashboardFor(thisWeek).pace).granularity)
        assertNull(dashboardFor(thisYear).pace)
    }

    @Test
    fun netSavings_isThereForAYearOnly() = runTest {
        dashboard.monthlyTrend.value = listOf(
            MonthlyTrend(year = 2026, month = 9, income = 900.0, expense = 400.0),
            MonthlyTrend(year = 2026, month = 10, income = 100.0, expense = 250.0)
        )

        val months = dashboardFor(thisYear).netSavings

        assertEquals(12, months.size)
        assertEquals(500.0, months[8].net, 0.001)
        assertEquals(-150.0, months[9].net, 0.001)
        assertEquals(listOf(true, true), months.takeLast(2).map { it.isFuture })

        assertTrue(dashboardFor(thisMonth).netSavings.isEmpty())
        assertTrue(dashboardFor(thisWeek).netSavings.isEmpty())
    }

    // --- Insights and live updates ---

    @Test
    fun insightsFollowTheData() = runTest {
        transactions.saveTransaction(
            transaction(id = 1, amount = 500.0, type = TransactionType.INCOME, dateMillis = october.startMillis)
        )
        dashboard.categoryBreakdown.value =
            listOf(CategoryBreakdown(categoryId = 1, categoryName = "Bills", iconName = "Bills", amount = 90.0))

        assertEquals(
            listOf(SmartInsight.Saved(500.0), SmartInsight.HighestSpending("Bills")),
            dashboardFor(thisMonth).insights
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
            dashboardFor(thisMonth).insights
        )
    }

    @Test
    fun aNewTransaction_updatesTheDashboardWithoutAskingAgain() = runTest {
        val emitted = mutableListOf<Dashboard>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            getDashboard()(thisMonth).collect { emitted += it }
        }
        assertEquals(0.0, emitted.last().summary.totalExpense, 0.001)

        transactions.saveTransaction(
            transaction(id = 1, amount = 80.0, type = TransactionType.EXPENSE, dateMillis = october.startMillis)
        )

        assertEquals(80.0, emitted.last().summary.totalExpense, 0.001)
        assertEquals(listOf(1), emitted.last().recentTransactions.map { it.id })
    }
}
