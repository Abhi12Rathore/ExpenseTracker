package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class GetCurrentPeriodUseCaseTest {

    // Thursday 8 Oct 2026
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 15, minute = 30)

    @Test
    fun month_isAnchoredAtTheFirstOfThisMonth() {
        assertEquals(
            DateFilter.Period(PeriodGranularity.MONTH, millisAt(2026, Calendar.OCTOBER, 1)),
            GetCurrentPeriodUseCase(FakeTimeProvider(now))(PeriodGranularity.MONTH)
        )
    }

    @Test
    fun year_isAnchoredAtNewYearsDay() {
        assertEquals(
            DateFilter.Period(PeriodGranularity.YEAR, millisAt(2026, Calendar.JANUARY, 1)),
            GetCurrentPeriodUseCase(FakeTimeProvider(now))(PeriodGranularity.YEAR)
        )
    }

    @Test
    fun week_isAnchoredAtTheConfiguredFirstDay() {
        assertEquals(
            DateFilter.Period(PeriodGranularity.WEEK, millisAt(2026, Calendar.OCTOBER, 4)),
            GetCurrentPeriodUseCase(FakeTimeProvider(now, firstDayOfWeek = Calendar.SUNDAY))(
                PeriodGranularity.WEEK
            )
        )
        assertEquals(
            DateFilter.Period(PeriodGranularity.WEEK, millisAt(2026, Calendar.OCTOBER, 5)),
            GetCurrentPeriodUseCase(FakeTimeProvider(now, firstDayOfWeek = Calendar.MONDAY))(
                PeriodGranularity.WEEK
            )
        )
    }

    @Test
    fun week_inTheFirstDaysOfJanuary_isAnchoredInThePreviousYear() {
        val newYearsDay = millisAt(2026, Calendar.JANUARY, 1, hour = 9)

        assertEquals(
            DateFilter.Period(PeriodGranularity.WEEK, millisAt(2025, Calendar.DECEMBER, 29)),
            GetCurrentPeriodUseCase(FakeTimeProvider(newYearsDay, firstDayOfWeek = Calendar.MONDAY))(
                PeriodGranularity.WEEK
            )
        )
    }
}
