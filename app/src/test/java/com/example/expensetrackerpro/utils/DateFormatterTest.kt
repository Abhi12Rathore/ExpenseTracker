package com.example.expensetrackerpro.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DateFormatterTest {

    private val originalLocale = Locale.getDefault()
    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis

    @Test
    fun formatDate_isDayMonthCommaYear() {
        assertEquals("08 Oct, 2026", DateFormatter.formatDate(millis(2026, Calendar.OCTOBER, 8)))
        assertEquals("31 Dec, 2025", DateFormatter.formatDate(millis(2025, Calendar.DECEMBER, 31)))
    }

    @Test
    fun formatMonthYear_isFullMonthName() {
        assertEquals("October 2026", DateFormatter.formatMonthYear(millis(2026, Calendar.OCTOBER, 8)))
        assertEquals("January 2027", DateFormatter.formatMonthYear(millis(2027, Calendar.JANUARY, 1)))
    }

    @Test
    fun usesDeviceTimeZone_forDayBoundary() {
        // 20:00 UTC on 31 Dec is already 1 Jan in Kolkata
        val utcEvening = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.DECEMBER, 31, 20, 0, 0)
        }.timeInMillis

        assertEquals("01 Jan, 2027", DateFormatter.formatDate(utcEvening))
        assertEquals("January 2027", DateFormatter.formatMonthYear(utcEvening))
    }

    @Test
    fun hourOfDay_is24Hour() {
        assertEquals(0, DateFormatter.hourOfDay(millis(2026, Calendar.OCTOBER, 8, hour = 0)))
        assertEquals(9, DateFormatter.hourOfDay(millis(2026, Calendar.OCTOBER, 8, hour = 9)))
        assertEquals(23, DateFormatter.hourOfDay(millis(2026, Calendar.OCTOBER, 8, hour = 23)))
    }
}
