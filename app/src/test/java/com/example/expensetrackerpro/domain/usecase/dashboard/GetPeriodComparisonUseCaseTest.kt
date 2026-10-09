package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.filter.GetComparableRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class GetPeriodComparisonUseCaseTest {

    private val transactions = FakeTransactionRepository()

    // Thursday 8 Oct 2026
    private val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 8, hour = 12))
    private val getDateRange = GetDateRangeUseCase(timeProvider)

    private val getPeriodComparison = GetPeriodComparisonUseCase(
        getFinancialSummary = GetFinancialSummaryUseCase(
            transactions,
            CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase())
        ),
        getDateRange = getDateRange,
        getComparableRange = GetComparableRangeUseCase(timeProvider, getDateRange, StepPeriodUseCase(timeProvider))
    )

    private var nextId = 1

    private suspend fun save(amount: Double, type: TransactionType, dateMillis: Long) {
        transactions.saveTransaction(
            transaction(id = nextId++, amount = amount, type = type, dateMillis = dateMillis)
        )
    }

    private fun period(granularity: PeriodGranularity, year: Int, month: Int, day: Int = 1) =
        DateFilter.Period(granularity, millisAt(year, month, day))

    // --- A period still under way ---

    @Test
    fun ongoingMonth_comparesWithTheSameDaysOfTheMonthBefore() = runTest {
        // By 8 Sep: saved 900. The rent on the 30th came later in the month
        save(900.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 5))
        save(400.0, TransactionType.EXPENSE, millisAt(2026, Calendar.SEPTEMBER, 30, hour = 23))
        // By 8 Oct: saved 750
        save(1000.0, TransactionType.INCOME, millisAt(2026, Calendar.OCTOBER, 1))
        save(250.0, TransactionType.EXPENSE, millisAt(2026, Calendar.OCTOBER, 7))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER)).first()

        assertEquals(
            PeriodComparison(
                granularity = PeriodGranularity.MONTH,
                previousRange = DateRange(
                    millisAt(2026, Calendar.SEPTEMBER, 1),
                    millisAt(2026, Calendar.SEPTEMBER, 9)
                ),
                balanceDifference = -150.0,
                isPartial = true
            ),
            comparison
        )
    }

    @Test
    fun ongoingMonth_countsTheSameDayOfTheMonthBefore_butNotTheNext() = runTest {
        save(100.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 8, hour = 23, minute = 59))
        save(5000.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 9))
        save(300.0, TransactionType.INCOME, millisAt(2026, Calendar.OCTOBER, 2))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER)).first()

        assertEquals(200.0, requireNotNull(comparison).balanceDifference, 0.001)
    }

    @Test
    fun ongoingWeek_comparesWithTheSameWeekdaysOfTheWeekBefore() = runTest {
        // Weeks start on Sunday: 27 Sep to 3 Oct, then 4 to 10 Oct. Today is Thursday
        save(200.0, TransactionType.EXPENSE, millisAt(2026, Calendar.SEPTEMBER, 28))
        // Friday of last week: later than today's weekday, so left out
        save(900.0, TransactionType.EXPENSE, millisAt(2026, Calendar.OCTOBER, 2))
        save(50.0, TransactionType.EXPENSE, millisAt(2026, Calendar.OCTOBER, 4))

        val comparison = getPeriodComparison(period(PeriodGranularity.WEEK, 2026, Calendar.OCTOBER, 4)).first()

        assertEquals(
            PeriodComparison(
                granularity = PeriodGranularity.WEEK,
                previousRange = DateRange(
                    millisAt(2026, Calendar.SEPTEMBER, 27),
                    millisAt(2026, Calendar.OCTOBER, 2)
                ),
                balanceDifference = 150.0,
                isPartial = true
            ),
            comparison
        )
    }

    @Test
    fun ongoingYear_comparesWithTheSameDatesLastYear() = runTest {
        save(5000.0, TransactionType.INCOME, millisAt(2025, Calendar.OCTOBER, 8, hour = 23))
        // After 8 Oct last year, so left out
        save(70000.0, TransactionType.INCOME, millisAt(2025, Calendar.DECEMBER, 31, hour = 23))
        save(8000.0, TransactionType.INCOME, millisAt(2026, Calendar.JANUARY, 1))

        val comparison = getPeriodComparison(period(PeriodGranularity.YEAR, 2026, Calendar.JANUARY)).first()

        assertEquals(
            PeriodComparison(
                granularity = PeriodGranularity.YEAR,
                previousRange = DateRange(
                    millisAt(2025, Calendar.JANUARY, 1),
                    millisAt(2025, Calendar.OCTOBER, 9)
                ),
                balanceDifference = 3000.0,
                isPartial = true
            ),
            comparison
        )
    }

    @Test
    fun anchorInsideThePeriod_makesNoDifference() = runTest {
        save(400.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 1))
        save(900.0, TransactionType.INCOME, millisAt(2026, Calendar.OCTOBER, 5))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER, day = 20)).first()

        assertEquals(500.0, requireNotNull(comparison).balanceDifference, 0.001)
    }

    // --- A finished period ---

    @Test
    fun finishedMonth_comparesWithTheWholeMonthBefore() = runTest {
        save(600.0, TransactionType.INCOME, millisAt(2026, Calendar.AUGUST, 31, hour = 23))
        save(900.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 5))
        save(400.0, TransactionType.EXPENSE, millisAt(2026, Calendar.SEPTEMBER, 30, hour = 23))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.SEPTEMBER)).first()

        assertEquals(
            PeriodComparison(
                granularity = PeriodGranularity.MONTH,
                previousRange = DateRange(
                    millisAt(2026, Calendar.AUGUST, 1),
                    millisAt(2026, Calendar.SEPTEMBER, 1)
                ),
                balanceDifference = -100.0,
                isPartial = false
            ),
            comparison
        )
    }

    @Test
    fun finishedYear_comparesWithTheWholeYearBefore() = runTest {
        save(5000.0, TransactionType.INCOME, millisAt(2024, Calendar.DECEMBER, 31, hour = 23))
        save(8000.0, TransactionType.INCOME, millisAt(2025, Calendar.JANUARY, 1))

        val comparison = getPeriodComparison(period(PeriodGranularity.YEAR, 2025, Calendar.JANUARY)).first()

        assertEquals(
            PeriodComparison(
                granularity = PeriodGranularity.YEAR,
                previousRange = DateRange(
                    millisAt(2024, Calendar.JANUARY, 1),
                    millisAt(2025, Calendar.JANUARY, 1)
                ),
                balanceDifference = 3000.0,
                isPartial = false
            ),
            comparison
        )
    }

    @Test
    fun finishedJanuary_comparesWithDecemberOfThePreviousYear() = runTest {
        save(100.0, TransactionType.INCOME, millisAt(2025, Calendar.DECEMBER, 25))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.JANUARY)).first()

        assertEquals(
            DateRange(millisAt(2025, Calendar.DECEMBER, 1), millisAt(2026, Calendar.JANUARY, 1)),
            requireNotNull(comparison).previousRange
        )
        assertEquals(-100.0, comparison.balanceDifference, 0.001)
    }

    // --- Signs and empty data ---

    @Test
    fun lessSavedThanBefore_isANegativeDifference() = runTest {
        save(1000.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 5))
        save(300.0, TransactionType.INCOME, millisAt(2026, Calendar.OCTOBER, 2))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER)).first()

        assertEquals(-700.0, requireNotNull(comparison).balanceDifference, 0.001)
    }

    @Test
    fun overspendingBothPeriods_comparesTheNegativeBalances() = runTest {
        save(800.0, TransactionType.EXPENSE, millisAt(2026, Calendar.SEPTEMBER, 5))
        save(300.0, TransactionType.EXPENSE, millisAt(2026, Calendar.OCTOBER, 2))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER)).first()

        // -300 against -800
        assertEquals(500.0, requireNotNull(comparison).balanceDifference, 0.001)
    }

    @Test
    fun noTransactionsInEitherPeriod_hasNothingToCompare() = runTest {
        assertNull(getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER)).first())
    }

    @Test
    fun onlyLaterTransactionsInThePreviousPeriod_stillHasNothingToCompare() = runTest {
        // September's only transaction falls after the 8th
        save(400.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 20))

        assertNull(getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER)).first())
    }

    @Test
    fun sameBalanceInBothPeriods_isAZeroDifference() = runTest {
        save(400.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 5))
        save(400.0, TransactionType.INCOME, millisAt(2026, Calendar.OCTOBER, 5))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER)).first()

        assertEquals(0.0, requireNotNull(comparison).balanceDifference, 0.001)
    }

    @Test
    fun customRange_hasNothingToCompareWith() = runTest {
        save(400.0, TransactionType.INCOME, millisAt(2026, Calendar.OCTOBER, 2))
        val custom = DateFilter.Custom(
            DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.OCTOBER, 8))
        )

        assertNull(getPeriodComparison(custom).first())
    }
}
