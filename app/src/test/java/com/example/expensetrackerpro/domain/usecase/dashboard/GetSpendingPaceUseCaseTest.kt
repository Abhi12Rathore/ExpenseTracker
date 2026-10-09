package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.SpendingPace
import com.example.expensetrackerpro.domain.usecase.filter.GetComparableRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GetSpendingPaceUseCaseTest {

    private val repository = FakeDashboardRepository()

    // Thursday 8 Oct 2026
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 15, minute = 30)

    private suspend fun pace(filter: DateFilter, nowMillis: Long = now): SpendingPace? {
        val timeProvider = FakeTimeProvider(nowMillis)
        val getDateRange = GetDateRangeUseCase(timeProvider)
        return GetSpendingPaceUseCase(
            repository,
            getDateRange,
            GetComparableRangeUseCase(timeProvider, getDateRange, StepPeriodUseCase(timeProvider)),
            timeProvider
        )(filter).first()
    }

    private suspend fun monthPace(year: Int, month: Int, nowMillis: Long = now): SpendingPace =
        requireNotNull(pace(DateFilter.Period(PeriodGranularity.MONTH, millisAt(year, month, 1)), nowMillis))

    private fun spent(year: Int, month: Int, day: Int, amount: Double, hour: Int = 10) =
        DailyExpense(dateMillis = millisAt(year, month, day, hour), amount = amount, transactionCount = 1)

    // --- Cumulative lines ---

    @Test
    fun current_accumulatesDayByDay_andStopsAtToday() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(2026, Calendar.OCTOBER, 2, 300.0),
            spent(2026, Calendar.OCTOBER, 5, 150.0, hour = 23),
            spent(2026, Calendar.OCTOBER, 8, 50.0, hour = 0)
        )

        val pace = monthPace(2026, Calendar.OCTOBER)

        assertEquals(
            listOf(0.0, 300.0, 300.0, 300.0, 450.0, 450.0, 450.0, 500.0),
            pace.current.map { it.total }
        )
        assertEquals(millisAt(2026, Calendar.OCTOBER, 1), pace.current.first().dateMillis)
        assertEquals(millisAt(2026, Calendar.OCTOBER, 8), pace.current.last().dateMillis)
    }

    @Test
    fun previous_coversTheWholePeriodBefore_evenWhileTheCurrentOneIsUnderWay() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(2026, Calendar.SEPTEMBER, 1, 100.0),
            spent(2026, Calendar.SEPTEMBER, 30, 900.0, hour = 23)
        )

        val pace = monthPace(2026, Calendar.OCTOBER)

        assertEquals(30, pace.previous.size)
        assertEquals(100.0, pace.previous[28].total, 0.001)
        assertEquals(1000.0, pace.previous.last().total, 0.001)
        assertEquals(millisAt(2026, Calendar.SEPTEMBER, 30), pace.previous.last().dateMillis)
    }

    @Test
    fun eachLine_onlyCountsItsOwnPeriod() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(2026, Calendar.AUGUST, 31, 7000.0, hour = 23),
            spent(2026, Calendar.SEPTEMBER, 10, 200.0),
            spent(2026, Calendar.OCTOBER, 1, 40.0, hour = 0)
        )

        val pace = monthPace(2026, Calendar.OCTOBER)

        assertEquals(40.0, pace.current.last().total, 0.001)
        assertEquals(200.0, pace.previous.last().total, 0.001)
    }

    @Test
    fun dayCount_isTheLongerOfTheTwoPeriods() = runTest {
        // October (31) after September (30)
        assertEquals(31, monthPace(2026, Calendar.OCTOBER).dayCount)
        // March (31) after February (28), seen once March is over
        assertEquals(31, monthPace(2026, Calendar.MARCH).dayCount)
        // November (30) after October (31), on its first day
        assertEquals(31, monthPace(2026, Calendar.NOVEMBER, millisAt(2026, Calendar.NOVEMBER, 1, hour = 9)).dayCount)
    }

    // --- Difference at the same day ---

    @Test
    fun ongoingMonth_differenceIsAgainstTheSameDayOfTheMonthBefore() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(2026, Calendar.SEPTEMBER, 3, 400.0),
            spent(2026, Calendar.SEPTEMBER, 8, 100.0, hour = 23),
            // After the 8th, so not yet spent "by this time last month"
            spent(2026, Calendar.SEPTEMBER, 9, 2000.0, hour = 0),
            spent(2026, Calendar.OCTOBER, 4, 350.0)
        )

        val pace = monthPace(2026, Calendar.OCTOBER)

        assertEquals(-150.0, pace.difference, 0.001)
        assertTrue(pace.isPartial)
        assertEquals(PeriodGranularity.MONTH, pace.granularity)
    }

    @Test
    fun spendingMoreThanBefore_isAPositiveDifference() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(2026, Calendar.SEPTEMBER, 3, 100.0),
            spent(2026, Calendar.OCTOBER, 4, 350.0)
        )

        assertEquals(250.0, monthPace(2026, Calendar.OCTOBER).difference, 0.001)
    }

    @Test
    fun endOfMarch_isComparedWithAllOfFebruary() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(2026, Calendar.FEBRUARY, 28, 600.0, hour = 23),
            spent(2026, Calendar.MARCH, 30, 1000.0)
        )

        val pace = monthPace(2026, Calendar.MARCH, millisAt(2026, Calendar.MARCH, 31, hour = 12))

        assertEquals(31, pace.current.size)
        assertEquals(28, pace.previous.size)
        assertEquals(400.0, pace.difference, 0.001)
    }

    @Test
    fun finishedMonth_comparesTheTotals_andDrawsBothInFull() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(2026, Calendar.AUGUST, 31, 900.0),
            spent(2026, Calendar.SEPTEMBER, 30, 500.0)
        )

        val pace = monthPace(2026, Calendar.SEPTEMBER)

        assertEquals(30, pace.current.size)
        assertEquals(31, pace.previous.size)
        assertEquals(-400.0, pace.difference, 0.001)
        assertFalse(pace.isPartial)
    }

    // --- Weeks ---

    @Test
    fun ongoingWeek_hasAPointPerWeekdaySoFar_againstAllSevenOfLastWeek() = runTest {
        // Weeks start on Sunday: 27 Sep to 3 Oct, then 4 to 10 Oct. Today is Thursday
        repository.dailyExpenses.value = listOf(
            spent(2026, Calendar.SEPTEMBER, 28, 200.0),
            // Saturday of last week: later than today's weekday
            spent(2026, Calendar.OCTOBER, 3, 900.0),
            spent(2026, Calendar.OCTOBER, 6, 120.0)
        )

        val pace = requireNotNull(
            pace(DateFilter.Period(PeriodGranularity.WEEK, millisAt(2026, Calendar.OCTOBER, 4)))
        )

        assertEquals(listOf(0.0, 0.0, 120.0, 120.0, 120.0), pace.current.map { it.total })
        assertEquals(listOf(0.0, 200.0, 200.0, 200.0, 200.0, 200.0, 1100.0), pace.previous.map { it.total })
        assertEquals(7, pace.dayCount)
        assertEquals(-80.0, pace.difference, 0.001)
        assertEquals(PeriodGranularity.WEEK, pace.granularity)
    }

    // --- Nothing to show ---

    @Test
    fun noExpenses_isTwoFlatLines_withNoSpending() = runTest {
        val pace = monthPace(2026, Calendar.OCTOBER)

        assertEquals(List(8) { 0.0 }, pace.current.map { it.total })
        assertEquals(List(30) { 0.0 }, pace.previous.map { it.total })
        assertEquals(0.0, pace.difference, 0.001)
        assertFalse(pace.hasSpending)
    }

    @Test
    fun spendingOnlyInThePreviousPeriod_stillCountsAsSpending() = runTest {
        repository.dailyExpenses.value = listOf(spent(2026, Calendar.SEPTEMBER, 20, 75.0))

        assertTrue(monthPace(2026, Calendar.OCTOBER).hasSpending)
    }

    @Test
    fun year_hasNoPace() = runTest {
        assertNull(pace(DateFilter.Period(PeriodGranularity.YEAR, millisAt(2026, Calendar.JANUARY, 1))))
    }

    @Test
    fun customRange_hasNoPace() = runTest {
        val custom = DateFilter.Custom(
            DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.OCTOBER, 8))
        )

        assertNull(pace(custom))
    }
}
