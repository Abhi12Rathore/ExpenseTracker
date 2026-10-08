package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class StepPeriodUseCaseTest {

    // Thursday 8 Oct 2026
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 15, minute = 30)

    private fun stepPeriod(nowMillis: Long = now, firstDayOfWeek: Int = Calendar.SUNDAY) =
        StepPeriodUseCase(FakeTimeProvider(nowMillis, firstDayOfWeek = firstDayOfWeek))

    private fun month(year: Int, month: Int, day: Int = 1) =
        DateFilter.Period(PeriodGranularity.MONTH, millisAt(year, month, day))

    private fun week(year: Int, month: Int, day: Int) =
        DateFilter.Period(PeriodGranularity.WEEK, millisAt(year, month, day))

    private fun year(year: Int) =
        DateFilter.Period(PeriodGranularity.YEAR, millisAt(year, Calendar.JANUARY, 1))

    // --- Month ---

    @Test
    fun month_back_isThePreviousMonth() {
        assertEquals(
            month(2026, Calendar.SEPTEMBER),
            stepPeriod()(month(2026, Calendar.OCTOBER), forward = false)
        )
    }

    @Test
    fun month_backFromJanuary_isDecemberOfThePreviousYear() {
        assertEquals(
            month(2025, Calendar.DECEMBER),
            stepPeriod()(month(2026, Calendar.JANUARY), forward = false)
        )
    }

    @Test
    fun month_forwardFromDecember_isJanuaryOfTheNextYear() {
        assertEquals(
            month(2026, Calendar.JANUARY),
            stepPeriod()(month(2025, Calendar.DECEMBER), forward = true)
        )
    }

    @Test
    fun month_anchoredOnThe31st_stepsToTheNeighbouringMonth() {
        val endOfMarch = DateFilter.Period(
            PeriodGranularity.MONTH,
            millisAt(2026, Calendar.MARCH, 31, hour = 18)
        )

        // Not skipped to March or May by the shorter months on either side
        assertEquals(month(2026, Calendar.FEBRUARY), stepPeriod()(endOfMarch, forward = false))
        assertEquals(month(2026, Calendar.APRIL), stepPeriod()(endOfMarch, forward = true))
    }

    // --- Week ---

    @Test
    fun week_stepsSevenDays() {
        assertEquals(
            week(2026, Calendar.SEPTEMBER, 27),
            stepPeriod()(week(2026, Calendar.OCTOBER, 4), forward = false)
        )
    }

    @Test
    fun week_stepsAcrossTheYearEnd() {
        val stepPeriod = stepPeriod(firstDayOfWeek = Calendar.MONDAY)
        val firstWeekOf2026 = week(2026, Calendar.JANUARY, 5)
        val lastWeekOf2025 = week(2025, Calendar.DECEMBER, 29)

        assertEquals(lastWeekOf2025, stepPeriod(firstWeekOf2026, forward = false))
        assertEquals(firstWeekOf2026, stepPeriod(lastWeekOf2025, forward = true))
    }

    @Test
    fun week_anchoredMidWeek_stepsToTheStartOfTheNeighbouringWeek() {
        // Wednesday 30 Sep 2026, in the week of Sunday 27 Sep
        val midWeek = DateFilter.Period(
            PeriodGranularity.WEEK,
            millisAt(2026, Calendar.SEPTEMBER, 30, hour = 13)
        )

        assertEquals(week(2026, Calendar.SEPTEMBER, 20), stepPeriod()(midWeek, forward = false))
        assertEquals(week(2026, Calendar.OCTOBER, 4), stepPeriod()(midWeek, forward = true))
    }

    // --- Year ---

    @Test
    fun year_stepsOneYear() {
        assertEquals(year(2025), stepPeriod()(year(2026), forward = false))
        assertEquals(year(2026), stepPeriod()(year(2025), forward = true))
    }

    @Test
    fun year_backFromALeapDayAnchor_isThePreviousYear() {
        val leapDay = DateFilter.Period(
            PeriodGranularity.YEAR,
            millisAt(2024, Calendar.FEBRUARY, 29, hour = 10)
        )

        assertEquals(year(2023), stepPeriod()(leapDay, forward = false))
    }

    // --- Future periods ---

    @Test
    fun forward_fromTheCurrentPeriod_isNotAllowed() {
        assertNull(stepPeriod()(month(2026, Calendar.OCTOBER), forward = true))
        assertNull(stepPeriod()(week(2026, Calendar.OCTOBER, 4), forward = true))
        assertNull(stepPeriod()(year(2026), forward = true))
    }

    @Test
    fun forward_intoTheCurrentPeriod_isAllowed() {
        assertEquals(
            month(2026, Calendar.OCTOBER),
            stepPeriod()(month(2026, Calendar.SEPTEMBER), forward = true)
        )
        assertEquals(
            week(2026, Calendar.OCTOBER, 4),
            stepPeriod()(week(2026, Calendar.SEPTEMBER, 27), forward = true)
        )
    }

    @Test
    fun forward_atTheLastMinuteOfTheYear_stillStopsAtTheYearEnd() {
        val newYearsEve = millisAt(2026, Calendar.DECEMBER, 31, hour = 23, minute = 59)

        assertNull(stepPeriod(newYearsEve)(month(2026, Calendar.DECEMBER), forward = true))
        assertNull(stepPeriod(newYearsEve)(year(2026), forward = true))
    }

    @Test
    fun forward_onTheFirstMomentOfAPeriod_reachesIt() {
        val newYear = millisAt(2027, Calendar.JANUARY, 1)

        assertEquals(
            month(2027, Calendar.JANUARY),
            stepPeriod(newYear)(month(2026, Calendar.DECEMBER), forward = true)
        )
        assertEquals(year(2027), stepPeriod(newYear)(year(2026), forward = true))
    }

    @Test
    fun steppingBackThenForward_returnsToTheStart() {
        val stepPeriod = stepPeriod()
        val start = month(2026, Calendar.FEBRUARY)

        var period = start
        repeat(14) { period = requireNotNull(stepPeriod(period, forward = false)) }
        assertEquals(month(2024, Calendar.DECEMBER), period)

        repeat(14) { period = requireNotNull(stepPeriod(period, forward = true)) }
        assertEquals(start, period)
    }
}
