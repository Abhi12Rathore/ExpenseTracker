package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.ComparableRange
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class GetComparableRangeUseCaseTest {

    private fun comparableRange(
        period: DateFilter.Period,
        nowMillis: Long,
        firstDayOfWeek: Int = Calendar.SUNDAY
    ): ComparableRange? {
        val timeProvider = FakeTimeProvider(nowMillis, firstDayOfWeek = firstDayOfWeek)
        return GetComparableRangeUseCase(
            timeProvider,
            GetDateRangeUseCase(timeProvider),
            StepPeriodUseCase(timeProvider)
        )(period)
    }

    private fun day(year: Int, month: Int, day: Int) = millisAt(year, month, day)

    private fun month(year: Int, month: Int) =
        DateFilter.Period(PeriodGranularity.MONTH, day(year, month, 1))

    private fun week(year: Int, month: Int, day: Int) =
        DateFilter.Period(PeriodGranularity.WEEK, day(year, month, day))

    private fun year(year: Int) =
        DateFilter.Period(PeriodGranularity.YEAR, day(year, Calendar.JANUARY, 1))

    private fun partial(start: Long, end: Long, wholeEnd: Long) = ComparableRange(
        range = DateRange(start, end),
        wholePeriod = DateRange(start, wholeEnd),
        isPartial = true
    )

    private fun whole(start: Long, end: Long) = ComparableRange(
        range = DateRange(start, end),
        wholePeriod = DateRange(start, end),
        isPartial = false
    )

    // --- Ongoing month ---

    @Test
    fun ongoingMonth_isComparedWithTheSameDaysOfTheMonthBefore() {
        // 1–9 Oct against 1–9 Sep
        assertEquals(
            partial(
                start = day(2026, Calendar.SEPTEMBER, 1),
                end = day(2026, Calendar.SEPTEMBER, 10),
                wholeEnd = day(2026, Calendar.OCTOBER, 1)
            ),
            comparableRange(month(2026, Calendar.OCTOBER), millisAt(2026, Calendar.OCTOBER, 9, hour = 15))
        )
    }

    @Test
    fun firstMomentOfAMonth_isComparedWithTheFirstDayOfTheMonthBefore() {
        assertEquals(
            DateRange(day(2026, Calendar.SEPTEMBER, 1), day(2026, Calendar.SEPTEMBER, 2)),
            comparableRange(month(2026, Calendar.OCTOBER), day(2026, Calendar.OCTOBER, 1))?.range
        )
    }

    @Test
    fun lastDayOfAMonth_reachesTheEndOfAShorterMonthBefore() {
        // 31 Oct against a September that only has 30 days
        assertEquals(
            partial(
                start = day(2026, Calendar.SEPTEMBER, 1),
                end = day(2026, Calendar.OCTOBER, 1),
                wholeEnd = day(2026, Calendar.OCTOBER, 1)
            ),
            comparableRange(month(2026, Calendar.OCTOBER), millisAt(2026, Calendar.OCTOBER, 31, hour = 23, minute = 59))
        )
    }

    @Test
    fun anchorInsideTheMonth_makesNoDifference() {
        val anchoredMidMonth = DateFilter.Period(PeriodGranularity.MONTH, millisAt(2026, Calendar.OCTOBER, 20, hour = 9))

        assertEquals(
            DateRange(day(2026, Calendar.SEPTEMBER, 1), day(2026, Calendar.SEPTEMBER, 10)),
            comparableRange(anchoredMidMonth, millisAt(2026, Calendar.OCTOBER, 9, hour = 15))?.range
        )
    }

    // --- Month-length clamping ---

    @Test
    fun endOfMarch_isClampedToTheWholeOfFebruary() {
        val wholeFebruary = DateRange(day(2026, Calendar.FEBRUARY, 1), day(2026, Calendar.MARCH, 1))

        // The 29th, 30th and 31st don't exist in February 2026
        listOf(29, 30, 31).forEach { dayOfMarch ->
            assertEquals(
                wholeFebruary,
                comparableRange(month(2026, Calendar.MARCH), millisAt(2026, Calendar.MARCH, dayOfMarch, hour = 10))?.range
            )
        }
    }

    @Test
    fun the28thOfMarch_isTheLastDayThatStillMatchesFebruary() {
        assertEquals(
            DateRange(day(2026, Calendar.FEBRUARY, 1), day(2026, Calendar.MARCH, 1)),
            comparableRange(month(2026, Calendar.MARCH), millisAt(2026, Calendar.MARCH, 28, hour = 10))?.range
        )
        assertEquals(
            DateRange(day(2026, Calendar.FEBRUARY, 1), day(2026, Calendar.FEBRUARY, 28)),
            comparableRange(month(2026, Calendar.MARCH), millisAt(2026, Calendar.MARCH, 27, hour = 10))?.range
        )
    }

    @Test
    fun endOfMarchInALeapYear_isClampedToThe29thOfFebruary() {
        assertEquals(
            DateRange(day(2024, Calendar.FEBRUARY, 1), day(2024, Calendar.MARCH, 1)),
            comparableRange(month(2024, Calendar.MARCH), millisAt(2024, Calendar.MARCH, 31, hour = 10))?.range
        )
        // The 29th exists that year, so it is matched rather than clamped
        assertEquals(
            DateRange(day(2024, Calendar.FEBRUARY, 1), day(2024, Calendar.MARCH, 1)),
            comparableRange(month(2024, Calendar.MARCH), millisAt(2024, Calendar.MARCH, 29, hour = 10))?.range
        )
        assertEquals(
            DateRange(day(2024, Calendar.FEBRUARY, 1), day(2024, Calendar.FEBRUARY, 29)),
            comparableRange(month(2024, Calendar.MARCH), millisAt(2024, Calendar.MARCH, 28, hour = 10))?.range
        )
    }

    @Test
    fun leapDay_inFebruary_isComparedWithThe29thOfJanuary() {
        assertEquals(
            DateRange(day(2024, Calendar.JANUARY, 1), day(2024, Calendar.JANUARY, 30)),
            comparableRange(month(2024, Calendar.FEBRUARY), millisAt(2024, Calendar.FEBRUARY, 29, hour = 10))?.range
        )
    }

    @Test
    fun january_isComparedWithDecemberOfTheYearBefore() {
        assertEquals(
            partial(
                start = day(2025, Calendar.DECEMBER, 1),
                end = day(2025, Calendar.DECEMBER, 16),
                wholeEnd = day(2026, Calendar.JANUARY, 1)
            ),
            comparableRange(month(2026, Calendar.JANUARY), millisAt(2026, Calendar.JANUARY, 15, hour = 8))
        )
    }

    // --- Ongoing week ---

    @Test
    fun ongoingWeek_startingSunday_isComparedWithTheSameWeekdays() {
        // Thursday 8 Oct 2026: Sun–Thu against Sun–Thu
        assertEquals(
            partial(
                start = day(2026, Calendar.SEPTEMBER, 27),
                end = day(2026, Calendar.OCTOBER, 2),
                wholeEnd = day(2026, Calendar.OCTOBER, 4)
            ),
            comparableRange(week(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 8, hour = 12))
        )
    }

    @Test
    fun ongoingWeek_startingMonday_isComparedWithTheSameWeekdays() {
        // Thursday 8 Oct 2026: Mon–Thu against Mon–Thu
        assertEquals(
            partial(
                start = day(2026, Calendar.SEPTEMBER, 28),
                end = day(2026, Calendar.OCTOBER, 2),
                wholeEnd = day(2026, Calendar.OCTOBER, 5)
            ),
            comparableRange(
                week(2026, Calendar.OCTOBER, 5),
                millisAt(2026, Calendar.OCTOBER, 8, hour = 12),
                firstDayOfWeek = Calendar.MONDAY
            )
        )
    }

    @Test
    fun firstDayOfAWeek_isComparedWithTheFirstDayOfTheWeekBefore() {
        assertEquals(
            DateRange(day(2026, Calendar.SEPTEMBER, 27), day(2026, Calendar.SEPTEMBER, 28)),
            comparableRange(week(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 4, hour = 7))?.range
        )
    }

    @Test
    fun lastDayOfAWeek_isComparedWithTheWholeWeekBefore() {
        assertEquals(
            DateRange(day(2026, Calendar.SEPTEMBER, 27), day(2026, Calendar.OCTOBER, 4)),
            comparableRange(week(2026, Calendar.OCTOBER, 4), millisAt(2026, Calendar.OCTOBER, 10, hour = 23))?.range
        )
    }

    @Test
    fun weekAcrossNewYear_isComparedWithTheLastFullWeekOfDecember() {
        // Friday 2 Jan 2026, in the week of Monday 29 Dec 2025: Mon–Fri against Mon–Fri
        assertEquals(
            partial(
                start = day(2025, Calendar.DECEMBER, 22),
                end = day(2025, Calendar.DECEMBER, 27),
                wholeEnd = day(2025, Calendar.DECEMBER, 29)
            ),
            comparableRange(
                week(2025, Calendar.DECEMBER, 29),
                millisAt(2026, Calendar.JANUARY, 2, hour = 12),
                firstDayOfWeek = Calendar.MONDAY
            )
        )
    }

    @Test
    fun firstWeekOfJanuary_isComparedWithAWeekThatStraddlesTheYearEnd() {
        // Wednesday 7 Jan 2026, week of Monday 5 Jan: against Mon 29 Dec – Wed 31 Dec
        assertEquals(
            partial(
                start = day(2025, Calendar.DECEMBER, 29),
                end = day(2026, Calendar.JANUARY, 1),
                wholeEnd = day(2026, Calendar.JANUARY, 5)
            ),
            comparableRange(
                week(2026, Calendar.JANUARY, 5),
                millisAt(2026, Calendar.JANUARY, 7, hour = 12),
                firstDayOfWeek = Calendar.MONDAY
            )
        )
    }

    // --- Ongoing year ---

    @Test
    fun ongoingYear_isComparedWithTheSameDatesLastYear() {
        // 1 Jan – 8 Oct 2026 against 1 Jan – 8 Oct 2025
        assertEquals(
            partial(
                start = day(2025, Calendar.JANUARY, 1),
                end = day(2025, Calendar.OCTOBER, 9),
                wholeEnd = day(2026, Calendar.JANUARY, 1)
            ),
            comparableRange(year(2026), millisAt(2026, Calendar.OCTOBER, 8, hour = 12))
        )
    }

    @Test
    fun leapDay_isComparedWithThe28thOfFebruaryLastYear() {
        // 2023 has no 29 Feb
        assertEquals(
            DateRange(day(2023, Calendar.JANUARY, 1), day(2023, Calendar.MARCH, 1)),
            comparableRange(year(2024), millisAt(2024, Calendar.FEBRUARY, 29, hour = 12))?.range
        )
    }

    @Test
    fun the28thOfFebruary_afterALeapYear_stopsBeforeTheLeapDay() {
        // 1 Jan – 28 Feb 2025 against 1 Jan – 28 Feb 2024, leaving 29 Feb 2024 out
        assertEquals(
            DateRange(day(2024, Calendar.JANUARY, 1), day(2024, Calendar.FEBRUARY, 29)),
            comparableRange(year(2025), millisAt(2025, Calendar.FEBRUARY, 28, hour = 12))?.range
        )
    }

    @Test
    fun firstOfMarch_afterALeapYear_includesTheLeapDay() {
        assertEquals(
            DateRange(day(2024, Calendar.JANUARY, 1), day(2024, Calendar.MARCH, 2)),
            comparableRange(year(2025), millisAt(2025, Calendar.MARCH, 1, hour = 12))?.range
        )
    }

    @Test
    fun newYearsEve_isComparedWithTheWholeYearBefore() {
        assertEquals(
            DateRange(day(2025, Calendar.JANUARY, 1), day(2026, Calendar.JANUARY, 1)),
            comparableRange(year(2026), millisAt(2026, Calendar.DECEMBER, 31, hour = 23, minute = 59))?.range
        )
    }

    @Test
    fun newYearsDay_isComparedWithNewYearsDayLastYear() {
        assertEquals(
            DateRange(day(2025, Calendar.JANUARY, 1), day(2025, Calendar.JANUARY, 2)),
            comparableRange(year(2026), millisAt(2026, Calendar.JANUARY, 1, hour = 1))?.range
        )
    }

    // --- Finished periods ---

    @Test
    fun finishedMonth_isComparedWithTheWholeMonthBefore() {
        val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)

        assertEquals(
            whole(day(2026, Calendar.AUGUST, 1), day(2026, Calendar.SEPTEMBER, 1)),
            comparableRange(month(2026, Calendar.SEPTEMBER), now)
        )
        // A 31-day month against a 28-day one, both in full
        assertEquals(
            whole(day(2026, Calendar.FEBRUARY, 1), day(2026, Calendar.MARCH, 1)),
            comparableRange(month(2026, Calendar.MARCH), now)
        )
    }

    @Test
    fun finishedWeek_isComparedWithTheWholeWeekBefore() {
        assertEquals(
            whole(day(2026, Calendar.SEPTEMBER, 20), day(2026, Calendar.SEPTEMBER, 27)),
            comparableRange(week(2026, Calendar.SEPTEMBER, 27), millisAt(2026, Calendar.OCTOBER, 8, hour = 12))
        )
    }

    @Test
    fun finishedYear_isComparedWithTheWholeYearBefore_leapOrNot() {
        val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)

        assertEquals(
            whole(day(2024, Calendar.JANUARY, 1), day(2025, Calendar.JANUARY, 1)),
            comparableRange(year(2025), now)
        )
    }

    @Test
    fun theMomentAPeriodEnds_itCountsAsFinished() {
        // Midnight on 1 Nov: October is over
        assertEquals(
            whole(day(2026, Calendar.SEPTEMBER, 1), day(2026, Calendar.OCTOBER, 1)),
            comparableRange(month(2026, Calendar.OCTOBER), day(2026, Calendar.NOVEMBER, 1))
        )
    }
}
