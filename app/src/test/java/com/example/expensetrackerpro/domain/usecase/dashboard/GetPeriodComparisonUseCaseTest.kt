package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.TransactionType
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

    private val getPeriodComparison = GetPeriodComparisonUseCase(
        getFinancialSummary = GetFinancialSummaryUseCase(
            transactions,
            CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase())
        ),
        getDateRange = GetDateRangeUseCase(timeProvider),
        stepPeriod = StepPeriodUseCase(timeProvider)
    )

    private var nextId = 1

    private suspend fun save(amount: Double, type: TransactionType, dateMillis: Long) {
        transactions.saveTransaction(
            transaction(id = nextId++, amount = amount, type = type, dateMillis = dateMillis)
        )
    }

    private fun period(granularity: PeriodGranularity, year: Int, month: Int, day: Int = 1) =
        DateFilter.Period(granularity, millisAt(year, month, day))

    @Test
    fun month_comparesWithTheMonthBefore() = runTest {
        // September: saved 500. October: saved 750
        save(900.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 5))
        save(400.0, TransactionType.EXPENSE, millisAt(2026, Calendar.SEPTEMBER, 30, hour = 23))
        save(1000.0, TransactionType.INCOME, millisAt(2026, Calendar.OCTOBER, 1))
        save(250.0, TransactionType.EXPENSE, millisAt(2026, Calendar.OCTOBER, 7))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER)).first()

        assertEquals(
            PeriodComparison(
                granularity = PeriodGranularity.MONTH,
                previousRange = DateRange(
                    millisAt(2026, Calendar.SEPTEMBER, 1),
                    millisAt(2026, Calendar.OCTOBER, 1)
                ),
                balanceDifference = 250.0
            ),
            comparison
        )
    }

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
    fun week_comparesWithTheWeekBefore() = runTest {
        // Weeks start on Sunday: 27 Sep to 3 Oct, then 4 to 10 Oct
        save(200.0, TransactionType.EXPENSE, millisAt(2026, Calendar.OCTOBER, 3, hour = 23))
        save(50.0, TransactionType.EXPENSE, millisAt(2026, Calendar.OCTOBER, 4))

        val comparison = getPeriodComparison(period(PeriodGranularity.WEEK, 2026, Calendar.OCTOBER, 4)).first()

        assertEquals(
            PeriodComparison(
                granularity = PeriodGranularity.WEEK,
                previousRange = DateRange(
                    millisAt(2026, Calendar.SEPTEMBER, 27),
                    millisAt(2026, Calendar.OCTOBER, 4)
                ),
                balanceDifference = 150.0
            ),
            comparison
        )
    }

    @Test
    fun year_comparesWithTheYearBefore() = runTest {
        save(5000.0, TransactionType.INCOME, millisAt(2025, Calendar.DECEMBER, 31, hour = 23))
        save(8000.0, TransactionType.INCOME, millisAt(2026, Calendar.JANUARY, 1))

        val comparison = getPeriodComparison(period(PeriodGranularity.YEAR, 2026, Calendar.JANUARY)).first()

        assertEquals(
            PeriodComparison(
                granularity = PeriodGranularity.YEAR,
                previousRange = DateRange(
                    millisAt(2025, Calendar.JANUARY, 1),
                    millisAt(2026, Calendar.JANUARY, 1)
                ),
                balanceDifference = 3000.0
            ),
            comparison
        )
    }

    @Test
    fun january_comparesWithDecemberOfThePreviousYear() = runTest {
        save(100.0, TransactionType.INCOME, millisAt(2025, Calendar.DECEMBER, 15))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.JANUARY)).first()

        assertEquals(
            DateRange(millisAt(2025, Calendar.DECEMBER, 1), millisAt(2026, Calendar.JANUARY, 1)),
            requireNotNull(comparison).previousRange
        )
        assertEquals(-100.0, comparison.balanceDifference, 0.001)
    }

    @Test
    fun anchorInsideThePeriod_stillComparesWholePeriods() = runTest {
        save(400.0, TransactionType.INCOME, millisAt(2026, Calendar.SEPTEMBER, 1))
        save(900.0, TransactionType.INCOME, millisAt(2026, Calendar.OCTOBER, 31, hour = 22))

        val comparison = getPeriodComparison(period(PeriodGranularity.MONTH, 2026, Calendar.OCTOBER, day = 20)).first()

        assertEquals(500.0, requireNotNull(comparison).balanceDifference, 0.001)
    }

    @Test
    fun noTransactionsInEitherPeriod_hasNothingToCompare() = runTest {
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
