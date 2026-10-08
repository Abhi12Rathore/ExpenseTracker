package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class GetDateRangeUseCaseTest {

    // The range depends only on the anchor; "now" just supplies the zone and week start
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 15, minute = 30)

    private fun rangeOf(
        granularity: PeriodGranularity,
        anchorMillis: Long,
        firstDayOfWeek: Int = Calendar.SUNDAY
    ): DateRange = GetDateRangeUseCase(FakeTimeProvider(now, firstDayOfWeek = firstDayOfWeek))(
        DateFilter.Period(granularity, anchorMillis)
    )

    // Thursday 8 Oct 2026
    private val thursday = millisAt(2026, Calendar.OCTOBER, 8, hour = 15, minute = 30)

    // --- Week ---

    @Test
    fun week_sundayFirst_runsSundayToSunday() {
        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 11)),
            rangeOf(PeriodGranularity.WEEK, thursday, firstDayOfWeek = Calendar.SUNDAY)
        )
    }

    @Test
    fun week_mondayFirst_runsMondayToMonday() {
        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 5), millisAt(2026, Calendar.OCTOBER, 12)),
            rangeOf(PeriodGranularity.WEEK, thursday, firstDayOfWeek = Calendar.MONDAY)
        )
    }

    @Test
    fun week_anchoredOnItsFirstMoment_startsThatDay() {
        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 11)),
            rangeOf(PeriodGranularity.WEEK, millisAt(2026, Calendar.OCTOBER, 4))
        )
    }

    @Test
    fun week_anchoredOnItsLastMinute_isTheSameWeek() {
        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 11)),
            rangeOf(PeriodGranularity.WEEK, millisAt(2026, Calendar.OCTOBER, 10, hour = 23, minute = 59))
        )
    }

    @Test
    fun week_canSpanAMonthEnd() {
        // With weeks starting on Monday, Sunday 4 Oct closes the week that began in September
        assertEquals(
            DateRange(millisAt(2026, Calendar.SEPTEMBER, 28), millisAt(2026, Calendar.OCTOBER, 5)),
            rangeOf(
                PeriodGranularity.WEEK,
                millisAt(2026, Calendar.OCTOBER, 4, hour = 9),
                firstDayOfWeek = Calendar.MONDAY
            )
        )
    }

    @Test
    fun week_canSpanAYearEnd() {
        // 1 Jan 2026 is a Thursday
        val newYearsDay = millisAt(2026, Calendar.JANUARY, 1, hour = 12)

        assertEquals(
            DateRange(millisAt(2025, Calendar.DECEMBER, 29), millisAt(2026, Calendar.JANUARY, 5)),
            rangeOf(PeriodGranularity.WEEK, newYearsDay, firstDayOfWeek = Calendar.MONDAY)
        )
        assertEquals(
            DateRange(millisAt(2025, Calendar.DECEMBER, 28), millisAt(2026, Calendar.JANUARY, 4)),
            rangeOf(PeriodGranularity.WEEK, newYearsDay, firstDayOfWeek = Calendar.SUNDAY)
        )
    }

    // --- Month ---

    @Test
    fun month_isFirstOfMonthToFirstOfNext() {
        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1)),
            rangeOf(PeriodGranularity.MONTH, thursday)
        )
    }

    @Test
    fun month_isTheSameFromItsFirstAndLastMinute() {
        val october =
            DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1))

        assertEquals(october, rangeOf(PeriodGranularity.MONTH, millisAt(2026, Calendar.OCTOBER, 1)))
        assertEquals(
            october,
            rangeOf(PeriodGranularity.MONTH, millisAt(2026, Calendar.OCTOBER, 31, hour = 23, minute = 59))
        )
    }

    @Test
    fun month_february_followsLeapYears() {
        assertEquals(
            DateRange(millisAt(2026, Calendar.FEBRUARY, 1), millisAt(2026, Calendar.MARCH, 1)),
            rangeOf(PeriodGranularity.MONTH, millisAt(2026, Calendar.FEBRUARY, 28, hour = 10))
        )
        assertEquals(
            DateRange(millisAt(2028, Calendar.FEBRUARY, 1), millisAt(2028, Calendar.MARCH, 1)),
            rangeOf(PeriodGranularity.MONTH, millisAt(2028, Calendar.FEBRUARY, 29, hour = 10))
        )
    }

    @Test
    fun month_december_endsInTheNextYear() {
        assertEquals(
            DateRange(millisAt(2026, Calendar.DECEMBER, 1), millisAt(2027, Calendar.JANUARY, 1)),
            rangeOf(PeriodGranularity.MONTH, millisAt(2026, Calendar.DECEMBER, 31, hour = 23))
        )
    }

    @Test
    fun month_january_startsOnNewYearsDay() {
        assertEquals(
            DateRange(millisAt(2027, Calendar.JANUARY, 1), millisAt(2027, Calendar.FEBRUARY, 1)),
            rangeOf(PeriodGranularity.MONTH, millisAt(2027, Calendar.JANUARY, 1))
        )
    }

    @Test
    fun neighbouringMonths_meetExactly() {
        val september = rangeOf(PeriodGranularity.MONTH, millisAt(2026, Calendar.SEPTEMBER, 15))
        val october = rangeOf(PeriodGranularity.MONTH, thursday)

        assertEquals(september.endMillis, october.startMillis)
    }

    // --- Year ---

    @Test
    fun year_isJanuaryFirstToNextJanuaryFirst() {
        val year2026 =
            DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2027, Calendar.JANUARY, 1))

        assertEquals(year2026, rangeOf(PeriodGranularity.YEAR, thursday))
        assertEquals(year2026, rangeOf(PeriodGranularity.YEAR, millisAt(2026, Calendar.JANUARY, 1)))
        assertEquals(
            year2026,
            rangeOf(PeriodGranularity.YEAR, millisAt(2026, Calendar.DECEMBER, 31, hour = 23, minute = 59))
        )
    }

    @Test
    fun year_leapYear_isOneDayLonger() {
        val leap = rangeOf(PeriodGranularity.YEAR, millisAt(2028, Calendar.JUNE, 1))

        assertEquals(
            DateRange(millisAt(2028, Calendar.JANUARY, 1), millisAt(2029, Calendar.JANUARY, 1)),
            leap
        )
        assertEquals(366L, (leap.endMillis - leap.startMillis) / (24 * 60 * 60 * 1000L))
    }

    // --- Custom ---

    @Test
    fun custom_isItsOwnRange() {
        val picked = DateRange(startMillis = 1_000L, endMillis = 2_000L)

        assertEquals(picked, GetDateRangeUseCase(FakeTimeProvider(now))(DateFilter.Custom(picked)))
    }
}
