package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GetSpendingBucketsUseCaseTest {

    private val repository = FakeDashboardRepository()

    // Thursday 8 Oct 2026
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 15, minute = 30)

    private fun day(year: Int, month: Int, day: Int) = millisAt(year, month, day)

    private fun month(year: Int, month: Int) = DateRange(
        millisAt(year, month, 1),
        if (month == Calendar.DECEMBER) millisAt(year + 1, Calendar.JANUARY, 1) else millisAt(year, month + 1, 1)
    )

    private suspend fun chart(range: DateRange, nowMillis: Long = now): SpendingChart =
        GetSpendingBucketsUseCase(repository, FakeTimeProvider(nowMillis))(range).first()

    private fun spent(dateMillis: Long, amount: Money, count: Int = 1) =
        DailyExpense(dateMillis = dateMillis, amount = amount, transactionCount = count)

    // --- Bucket filling ---

    @Test
    fun asksTheRepositoryForTheWholeRange() = runTest {
        val october = month(2026, Calendar.OCTOBER)

        chart(october)

        assertEquals(october, repository.dailyExpensesRange)
    }

    @Test
    fun noExpenses_stillHasABucketForEveryDay_allZero() = runTest {
        val chart = chart(month(2026, Calendar.OCTOBER))

        assertEquals(BucketSize.DAY, chart.bucketSize)
        assertEquals(31, chart.buckets.size)
        assertTrue(chart.buckets.all { it.amount == Money.ZERO && it.transactionCount == 0 })
        assertFalse(chart.hasSpending)
        assertEquals(Money.ZERO, chart.average)
    }

    @Test
    fun daysWithoutExpenses_areFilledWithZeroBetweenTheOnesWithData() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(millisAt(2026, Calendar.OCTOBER, 2, hour = 9), rupees(250), count = 2),
            spent(millisAt(2026, Calendar.OCTOBER, 5, hour = 23, minute = 59), rupees(100))
        )

        val chart = chart(month(2026, Calendar.OCTOBER))

        assertEquals(
            listOf(Money.ZERO, rupees(250), Money.ZERO, Money.ZERO, rupees(100), Money.ZERO),
            chart.buckets.take(6).map { it.amount }
        )
        assertEquals(listOf(0, 2, 0, 0, 1, 0), chart.buckets.take(6).map { it.transactionCount })
        assertTrue(chart.hasSpending)
    }

    @Test
    fun bucketsAreConsecutiveWholeDays_coveringTheRangeExactly() = runTest {
        val october = month(2026, Calendar.OCTOBER)

        val buckets = chart(october).buckets

        assertEquals(october.startMillis, buckets.first().range.startMillis)
        assertEquals(october.endMillis, buckets.last().range.endMillis)
        buckets.zipWithNext { earlier, later ->
            assertEquals(earlier.range.endMillis, later.range.startMillis)
        }
        assertEquals(
            DateRange(day(2026, Calendar.OCTOBER, 17), day(2026, Calendar.OCTOBER, 18)),
            buckets[16].range
        )
    }

    @Test
    fun week_hasSevenDayBuckets() = runTest {
        val week = DateRange(day(2026, Calendar.OCTOBER, 4), day(2026, Calendar.OCTOBER, 11))

        val chart = chart(week)

        assertEquals(BucketSize.DAY, chart.bucketSize)
        assertEquals(7, chart.buckets.size)
    }

    // --- Month lengths ---

    @Test
    fun monthLengths_followTheCalendar() = runTest {
        assertEquals(31, chart(month(2026, Calendar.JANUARY)).buckets.size)
        assertEquals(28, chart(month(2026, Calendar.FEBRUARY)).buckets.size)
        assertEquals(30, chart(month(2026, Calendar.APRIL)).buckets.size)
        assertEquals(30, chart(month(2026, Calendar.SEPTEMBER)).buckets.size)
    }

    @Test
    fun februaryInALeapYear_has29Buckets_andKeepsTheLeapDaysSpending() = runTest {
        repository.dailyExpenses.value = listOf(spent(millisAt(2024, Calendar.FEBRUARY, 29, hour = 20), rupees(75)))

        val chart = chart(month(2024, Calendar.FEBRUARY))

        assertEquals(29, chart.buckets.size)
        assertEquals(rupees(75), chart.buckets.last().amount)
    }

    // --- Year boundaries ---

    @Test
    fun weekAcrossNewYear_keepsEachDayInItsOwnBucket() = runTest {
        // Monday 29 Dec 2025 to Sunday 4 Jan 2026
        val week = DateRange(day(2025, Calendar.DECEMBER, 29), day(2026, Calendar.JANUARY, 5))
        repository.dailyExpenses.value = listOf(
            spent(millisAt(2025, Calendar.DECEMBER, 31, hour = 23, minute = 59), rupees(500)),
            spent(millisAt(2026, Calendar.JANUARY, 1), rupees(40))
        )

        val chart = chart(week)

        assertEquals(7, chart.buckets.size)
        assertEquals(listOf(Money.ZERO, Money.ZERO, rupees(500), rupees(40), Money.ZERO, Money.ZERO, Money.ZERO), chart.buckets.map { it.amount })
        assertEquals(
            DateRange(day(2025, Calendar.DECEMBER, 31), day(2026, Calendar.JANUARY, 1)),
            chart.buckets[2].range
        )
    }

    @Test
    fun year_hasTwelveCalendarMonthBuckets() = runTest {
        val year = DateRange(day(2026, Calendar.JANUARY, 1), day(2027, Calendar.JANUARY, 1))

        val chart = chart(year)

        assertEquals(BucketSize.MONTH, chart.bucketSize)
        assertEquals(
            (Calendar.JANUARY..Calendar.DECEMBER).map { month(2026, it) },
            chart.buckets.map { it.range }
        )
    }

    @Test
    fun year_sumsEachMonthsDays_andLeavesEmptyMonthsAtZero() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(millisAt(2026, Calendar.JANUARY, 31, hour = 23), rupees(100), count = 2),
            spent(millisAt(2026, Calendar.FEBRUARY, 1), rupees(30)),
            spent(millisAt(2026, Calendar.FEBRUARY, 28, hour = 12), rupees(20), count = 3),
            spent(millisAt(2026, Calendar.DECEMBER, 31, hour = 23, minute = 59), rupees(900))
        )

        val chart = chart(DateRange(day(2026, Calendar.JANUARY, 1), day(2027, Calendar.JANUARY, 1)))

        assertEquals(
            listOf(rupees(100), rupees(50)) + List(9) { Money.ZERO } + rupees(900),
            chart.buckets.map { it.amount }
        )
        assertEquals(listOf(2, 4), chart.buckets.take(2).map { it.transactionCount })
    }

    @Test
    fun longCustomRangeAcrossNewYear_clipsTheFirstAndLastMonth() = runTest {
        // 15 Nov 2025 to 10 Feb 2026 inclusive
        val range = DateRange(day(2025, Calendar.NOVEMBER, 15), day(2026, Calendar.FEBRUARY, 11))

        val chart = chart(range)

        assertEquals(BucketSize.MONTH, chart.bucketSize)
        assertEquals(
            listOf(
                DateRange(day(2025, Calendar.NOVEMBER, 15), day(2025, Calendar.DECEMBER, 1)),
                month(2025, Calendar.DECEMBER),
                month(2026, Calendar.JANUARY),
                DateRange(day(2026, Calendar.FEBRUARY, 1), day(2026, Calendar.FEBRUARY, 11))
            ),
            chart.buckets.map { it.range }
        )
    }

    // --- Daily or monthly ---

    @Test
    fun customRange_upTo62Days_isDaily_andBeyondThatMonthly() = runTest {
        val start = day(2026, Calendar.JULY, 1)

        // July and August are 31 days each
        val sixtyTwoDays = chart(DateRange(start, day(2026, Calendar.SEPTEMBER, 1)))
        assertEquals(BucketSize.DAY, sixtyTwoDays.bucketSize)
        assertEquals(62, sixtyTwoDays.buckets.size)

        val sixtyThreeDays = chart(DateRange(start, day(2026, Calendar.SEPTEMBER, 2)))
        assertEquals(BucketSize.MONTH, sixtyThreeDays.bucketSize)
        assertEquals(3, sixtyThreeDays.buckets.size)
    }

    @Test
    fun singleDayRange_isOneBucket() = runTest {
        val today = DateRange(day(2026, Calendar.OCTOBER, 8), day(2026, Calendar.OCTOBER, 9))

        assertEquals(listOf(today), chart(today).buckets.map { it.range })
    }

    // --- Future buckets and the average ---

    @Test
    fun bucketsAfterToday_areFuture_andTodayIsNot() = runTest {
        val buckets = chart(month(2026, Calendar.OCTOBER)).buckets

        assertTrue(buckets.take(8).none { it.isFuture })
        assertTrue(buckets.drop(8).all { it.isFuture })
    }

    @Test
    fun pastPeriod_hasNoFutureBuckets() = runTest {
        assertTrue(chart(month(2026, Calendar.SEPTEMBER)).buckets.none { it.isFuture })
    }

    @Test
    fun year_marksTheMonthsThatHaveNotStarted() = runTest {
        val chart = chart(DateRange(day(2026, Calendar.JANUARY, 1), day(2027, Calendar.JANUARY, 1)))

        // October is under way; November and December are not
        assertEquals(List(10) { false } + listOf(true, true), chart.buckets.map { it.isFuture })
    }

    @Test
    fun average_isOverTheBucketsThatHaveStarted_includingEmptyOnes() = runTest {
        repository.dailyExpenses.value = listOf(
            spent(day(2026, Calendar.OCTOBER, 1), rupees(300)),
            spent(day(2026, Calendar.OCTOBER, 7), rupees(500))
        )

        // 800 over the eight days up to today, not over all 31
        assertEquals(rupees(100), chart(month(2026, Calendar.OCTOBER)).average)
    }

    @Test
    fun average_ofAPastMonth_isOverTheWholeMonth() = runTest {
        repository.dailyExpenses.value = listOf(spent(day(2026, Calendar.SEPTEMBER, 10), rupees(600)))

        assertEquals(rupees(20), chart(month(2026, Calendar.SEPTEMBER)).average)
    }

    @Test
    fun periodThatHasNotStarted_hasNoAverage() = runTest {
        val chart = chart(month(2026, Calendar.NOVEMBER))

        assertTrue(chart.buckets.all { it.isFuture })
        assertEquals(Money.ZERO, chart.average)
    }

    @Test
    fun newExpenses_updateTheBuckets() = runTest {
        val october = month(2026, Calendar.OCTOBER)
        val useCase = GetSpendingBucketsUseCase(repository, FakeTimeProvider(now))
        assertEquals(Money.ZERO, useCase(october).first().buckets[0].amount)

        repository.dailyExpenses.value = listOf(spent(day(2026, Calendar.OCTOBER, 1), rupees(120)))

        assertEquals(rupees(120), useCase(october).first().buckets[0].amount)
    }
}
