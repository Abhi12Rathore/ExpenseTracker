package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.DateSelection
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ResolveTransactionDateUseCaseTest {

    private val utc = TimeZone.getTimeZone("UTC")

    // Thursday 8 Oct 2026, 12:30 in Kolkata
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12, minute = 30)
    private val resolve = ResolveTransactionDateUseCase(FakeTimeProvider(now))

    private fun utcDay(year: Int, month: Int, day: Int) = millisAt(year, month, day, timeZone = utc)

    @Test
    fun today_isNow() {
        val date = resolve(DateSelection.Today)

        assertEquals(now, date.millis)
        assertEquals(DateChoice.TODAY, date.choice)
        assertEquals(utcDay(2026, Calendar.OCTOBER, 8), date.pickerUtcMillis)
    }

    @Test
    fun yesterday_isTheSameTimeOneDayEarlier() {
        val date = resolve(DateSelection.Yesterday)

        assertEquals(millisAt(2026, Calendar.OCTOBER, 7, hour = 12, minute = 30), date.millis)
        assertEquals(DateChoice.YESTERDAY, date.choice)
        assertEquals(utcDay(2026, Calendar.OCTOBER, 7), date.pickerUtcMillis)
    }

    @Test
    fun pickedDate_isThatLocalDayAtTheCurrentTime() {
        val date = resolve(DateSelection.Picked(utcDay(2026, Calendar.SEPTEMBER, 30)))

        assertEquals(millisAt(2026, Calendar.SEPTEMBER, 30, hour = 12, minute = 30), date.millis)
        assertEquals(DateChoice.OTHER, date.choice)
        assertEquals(utcDay(2026, Calendar.SEPTEMBER, 30), date.pickerUtcMillis)
    }

    @Test
    fun pickingTodayOrYesterday_selectsThatShortcut() {
        assertEquals(DateChoice.TODAY, resolve(DateSelection.Picked(utcDay(2026, Calendar.OCTOBER, 8))).choice)
        assertEquals(DateChoice.YESTERDAY, resolve(DateSelection.Picked(utcDay(2026, Calendar.OCTOBER, 7))).choice)
    }

    @Test
    fun pickedFutureDate_isBroughtBackToNow() {
        val date = resolve(DateSelection.Picked(utcDay(2026, Calendar.OCTOBER, 20)))

        assertEquals(now, date.millis)
        assertEquals(DateChoice.TODAY, date.choice)
    }

    @Test
    fun storedDate_isKeptExactly() {
        val stored = millisAt(2026, Calendar.OCTOBER, 7, hour = 23, minute = 59)
        val date = resolve(DateSelection.Stored(stored))

        assertEquals(stored, date.millis)
        assertEquals(DateChoice.YESTERDAY, date.choice)
    }

    @Test
    fun latestPickerDay_isTodaysDate() {
        assertEquals(utcDay(2026, Calendar.OCTOBER, 8), resolve(DateSelection.Today).maxPickerUtcMillis)
    }

    @Test
    fun westOfUtc_aPickedDateStaysOnTheDayThatWasPicked() {
        val newYork = TimeZone.getTimeZone("America/New_York")
        val evening = millisAt(2026, Calendar.OCTOBER, 8, hour = 20, timeZone = newYork)
        val resolveInNewYork = ResolveTransactionDateUseCase(FakeTimeProvider(evening, newYork))

        val date = resolveInNewYork(DateSelection.Picked(utcDay(2026, Calendar.OCTOBER, 5)))

        assertEquals(millisAt(2026, Calendar.OCTOBER, 5, hour = 20, timeZone = newYork), date.millis)
        // Already 9 Oct in UTC, but the picker must still stop at the local date
        assertEquals(utcDay(2026, Calendar.OCTOBER, 8), date.maxPickerUtcMillis)
    }
}
