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
    fun formatYear_isFourDigits() {
        assertEquals("2026", DateFormatter.formatYear(millis(2026, Calendar.OCTOBER, 8)))
    }

    @Test
    fun formatShortMonthYear_isAbbreviatedMonth() {
        assertEquals("Oct 2026", DateFormatter.formatShortMonthYear(millis(2026, Calendar.OCTOBER, 8)))
    }

    @Test
    fun formatDayRange_withinAMonth_namesTheMonthOnce() {
        val start = millis(2026, Calendar.OCTOBER, 1, hour = 0)
        val end = millis(2026, Calendar.OCTOBER, 31, hour = 23)

        assertEquals("1 – 31 Oct", DateFormatter.formatDayRange(start, end, withYear = false))
        assertEquals("1 – 31 Oct 2026", DateFormatter.formatDayRange(start, end, withYear = true))
    }

    @Test
    fun formatDayRange_acrossMonths_namesBoth() {
        val start = millis(2026, Calendar.SEPTEMBER, 28)
        val end = millis(2026, Calendar.OCTOBER, 4)

        assertEquals("28 Sep – 4 Oct", DateFormatter.formatDayRange(start, end, withYear = false))
        assertEquals("28 Sep – 4 Oct 2026", DateFormatter.formatDayRange(start, end, withYear = true))
        assertEquals(
            "1 Jan – 31 Dec",
            DateFormatter.formatDayRange(
                millis(2026, Calendar.JANUARY, 1),
                millis(2026, Calendar.DECEMBER, 31),
                withYear = false
            )
        )
    }

    @Test
    fun formatDayRange_acrossYears_alwaysShowsBothYears() {
        val start = millis(2025, Calendar.DECEMBER, 29)
        val end = millis(2026, Calendar.JANUARY, 4)

        assertEquals("29 Dec 2025 – 4 Jan 2026", DateFormatter.formatDayRange(start, end, withYear = false))
        assertEquals("29 Dec 2025 – 4 Jan 2026", DateFormatter.formatDayRange(start, end, withYear = true))
    }

    @Test
    fun formatDayRange_singleDay_isJustThatDay() {
        val start = millis(2026, Calendar.OCTOBER, 8, hour = 0)
        val end = millis(2026, Calendar.OCTOBER, 8, hour = 23)

        assertEquals("8 Oct", DateFormatter.formatDayRange(start, end, withYear = false))
        assertEquals("8 Oct 2026", DateFormatter.formatDayRange(start, end, withYear = true))
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
    fun chartLabels_areShortForms() {
        // Saturday 17 Oct 2026
        val day = millis(2026, Calendar.OCTOBER, 17)

        assertEquals("Oct", DateFormatter.formatShortMonth(day))
        assertEquals("O", DateFormatter.formatMonthInitial(day))
        assertEquals("Sat", DateFormatter.formatWeekday(day))
        assertEquals("17", DateFormatter.formatDayOfMonth(day))
        assertEquals("17 Oct", DateFormatter.formatDayMonth(day))
        assertEquals("Sat, 17 Oct", DateFormatter.formatWeekdayDayMonth(day))
        assertEquals("Sat 17 Oct", DateFormatter.formatDayHeader(day))
    }

    @Test
    fun formatDayOfMonth_hasNoLeadingZero() {
        assertEquals("8", DateFormatter.formatDayOfMonth(millis(2026, Calendar.OCTOBER, 8)))
    }

    @Test
    fun formatTime_isTwelveHourClockWithoutALeadingZero() {
        Locale.setDefault(Locale.forLanguageTag("en-IN"))

        assertEquals("4:20 pm", DateFormatter.formatTime(millis(2026, Calendar.OCTOBER, 11, hour = 16) + 20 * 60_000))
        assertEquals("9:00 am", DateFormatter.formatTime(millis(2026, Calendar.OCTOBER, 11, hour = 9)))
        assertEquals("12:00 am", DateFormatter.formatTime(millis(2026, Calendar.OCTOBER, 11, hour = 0)))
    }

    @Test
    fun formatDateTime_isDayMonthYearThenTheTime() {
        Locale.setDefault(Locale.forLanguageTag("en-IN"))

        assertEquals(
            "11 Oct 2026, 4:20 pm",
            DateFormatter.formatDateTime(millis(2026, Calendar.OCTOBER, 11, hour = 16) + 20 * 60_000)
        )
    }

    @Test
    fun formatMonthRange_repeatsOnlyWhatDiffers() {
        val jan = millis(2026, Calendar.JANUARY, 1)
        val oct = millis(2026, Calendar.OCTOBER, 11)

        assertEquals("Jan – Oct 2026", DateFormatter.formatMonthRange(jan, oct))
        assertEquals("Oct 2026", DateFormatter.formatMonthRange(millis(2026, Calendar.OCTOBER, 1), oct))
        assertEquals("Oct 2026", DateFormatter.formatMonthRange(oct, oct))
        assertEquals("Nov 2025 – Oct 2026", DateFormatter.formatMonthRange(millis(2025, Calendar.NOVEMBER, 30), oct))
        assertEquals("Oct 2025 – Oct 2026", DateFormatter.formatMonthRange(millis(2025, Calendar.OCTOBER, 11), oct))
    }

    @Test
    fun formatIsoDate_isYearMonthDay_zeroPadded() {
        assertEquals("2026-10-08", DateFormatter.formatIsoDate(millis(2026, Calendar.OCTOBER, 8)))
        assertEquals("2027-01-03", DateFormatter.formatIsoDate(millis(2027, Calendar.JANUARY, 3)))
    }

    @Test
    fun formatIsoDate_usesWesternDigits_whateverTheLanguage() {
        val day = millis(2026, Calendar.OCTOBER, 8)

        listOf("ar-EG", "hi-IN-u-nu-deva", "fa-IR").forEach { tag ->
            Locale.setDefault(Locale.forLanguageTag(tag))

            assertEquals("2026-10-08", DateFormatter.formatIsoDate(day))
        }
    }
}
