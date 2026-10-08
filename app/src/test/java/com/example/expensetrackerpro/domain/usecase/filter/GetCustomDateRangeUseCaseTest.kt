package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GetCustomDateRangeUseCaseTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val newYork = TimeZone.getTimeZone("America/New_York")

    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)

    /** What a date picker reports for a chosen date. */
    private fun picked(year: Int, month: Int, day: Int) = millisAt(year, month, day, timeZone = utc)

    @Test
    fun pickedDates_becomeWholeLocalDays() {
        val range = GetCustomDateRangeUseCase(FakeTimeProvider(now))(
            picked(2026, Calendar.OCTOBER, 8),
            picked(2026, Calendar.OCTOBER, 10)
        )

        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 8), millisAt(2026, Calendar.OCTOBER, 11)),
            range
        )
    }

    @Test
    fun sameStartAndEnd_isThatSingleDay() {
        val range = GetCustomDateRangeUseCase(FakeTimeProvider(now))(
            picked(2026, Calendar.OCTOBER, 8),
            picked(2026, Calendar.OCTOBER, 8)
        )

        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 8), millisAt(2026, Calendar.OCTOBER, 9)),
            range
        )
    }

    @Test
    fun rangeCanCrossTheYearBoundary() {
        val range = GetCustomDateRangeUseCase(FakeTimeProvider(now))(
            picked(2026, Calendar.DECEMBER, 30),
            picked(2026, Calendar.DECEMBER, 31)
        )

        assertEquals(
            DateRange(millisAt(2026, Calendar.DECEMBER, 30), millisAt(2027, Calendar.JANUARY, 1)),
            range
        )
    }

    @Test
    fun zonesBehindUtc_keepTheChosenDates() {
        // Midnight UTC is still the previous evening in New York; the dates must not slip a day
        val range = GetCustomDateRangeUseCase(FakeTimeProvider(now, timeZone = newYork))(
            picked(2026, Calendar.OCTOBER, 8),
            picked(2026, Calendar.OCTOBER, 10)
        )

        assertEquals(
            DateRange(
                millisAt(2026, Calendar.OCTOBER, 8, timeZone = newYork),
                millisAt(2026, Calendar.OCTOBER, 11, timeZone = newYork)
            ),
            range
        )
    }
}
