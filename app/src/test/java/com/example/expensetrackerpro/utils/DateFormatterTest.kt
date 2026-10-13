package com.example.expensetrackerpro.utils

import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.HINDI
import com.example.expensetrackerpro.fakes.dateFormatter
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DateFormatterTest {

    private val originalLocale = Locale.getDefault()
    private val originalTimeZone = TimeZone.getDefault()

    // The app in English, whatever the phone is set to
    private val formatter = dateFormatter(Locale.US)
    private val hindiFormatter = dateFormatter(HINDI)

    @Before
    fun setUp() {
        // The phone in another language: no date may follow it
        Locale.setDefault(Locale.FRANCE)
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
        assertEquals("08 Oct, 2026", formatter.formatDate(millis(2026, Calendar.OCTOBER, 8)))
        assertEquals("31 Dec, 2025", formatter.formatDate(millis(2025, Calendar.DECEMBER, 31)))
    }

    @Test
    fun formatMonthYear_isFullMonthName() {
        assertEquals("October 2026", formatter.formatMonthYear(millis(2026, Calendar.OCTOBER, 8)))
        assertEquals("January 2027", formatter.formatMonthYear(millis(2027, Calendar.JANUARY, 1)))
    }

    @Test
    fun formatYear_isFourDigits() {
        assertEquals("2026", formatter.formatYear(millis(2026, Calendar.OCTOBER, 8)))
    }

    @Test
    fun formatShortMonthYear_isAbbreviatedMonth() {
        assertEquals("Oct 2026", formatter.formatShortMonthYear(millis(2026, Calendar.OCTOBER, 8)))
    }

    @Test
    fun formatDayRange_withinAMonth_namesTheMonthOnce() {
        val start = millis(2026, Calendar.OCTOBER, 1, hour = 0)
        val end = millis(2026, Calendar.OCTOBER, 31, hour = 23)

        assertEquals("1 – 31 Oct", formatter.formatDayRange(start, end, withYear = false))
        assertEquals("1 – 31 Oct 2026", formatter.formatDayRange(start, end, withYear = true))
    }

    @Test
    fun formatDayRange_acrossMonths_namesBoth() {
        val start = millis(2026, Calendar.SEPTEMBER, 28)
        val end = millis(2026, Calendar.OCTOBER, 4)

        assertEquals("28 Sep – 4 Oct", formatter.formatDayRange(start, end, withYear = false))
        assertEquals("28 Sep – 4 Oct 2026", formatter.formatDayRange(start, end, withYear = true))
        assertEquals(
            "1 Jan – 31 Dec",
            formatter.formatDayRange(
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

        assertEquals("29 Dec 2025 – 4 Jan 2026", formatter.formatDayRange(start, end, withYear = false))
        assertEquals("29 Dec 2025 – 4 Jan 2026", formatter.formatDayRange(start, end, withYear = true))
    }

    @Test
    fun formatDayRange_singleDay_isJustThatDay() {
        val start = millis(2026, Calendar.OCTOBER, 8, hour = 0)
        val end = millis(2026, Calendar.OCTOBER, 8, hour = 23)

        assertEquals("8 Oct", formatter.formatDayRange(start, end, withYear = false))
        assertEquals("8 Oct 2026", formatter.formatDayRange(start, end, withYear = true))
    }

    @Test
    fun usesDeviceTimeZone_forDayBoundary() {
        // 20:00 UTC on 31 Dec is already 1 Jan in Kolkata
        val utcEvening = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.DECEMBER, 31, 20, 0, 0)
        }.timeInMillis

        assertEquals("01 Jan, 2027", formatter.formatDate(utcEvening))
        assertEquals("January 2027", formatter.formatMonthYear(utcEvening))
    }

    @Test
    fun chartLabels_areShortForms() {
        // Saturday 17 Oct 2026
        val day = millis(2026, Calendar.OCTOBER, 17)

        assertEquals("Oct", formatter.formatShortMonth(day))
        assertEquals("O", formatter.formatMonthInitial(day))
        assertEquals("Sat", formatter.formatWeekday(day))
        assertEquals("17", formatter.formatDayOfMonth(day))
        assertEquals("17 Oct", formatter.formatDayMonth(day))
        assertEquals("Sat, 17 Oct", formatter.formatWeekdayDayMonth(day))
        assertEquals("Sat 17 Oct", formatter.formatDayHeader(day))
    }

    @Test
    fun formatDayOfMonth_hasNoLeadingZero() {
        assertEquals("8", formatter.formatDayOfMonth(millis(2026, Calendar.OCTOBER, 8)))
    }

    @Test
    fun formatTime_isTwelveHourClockWithoutALeadingZero() {
        // English as written in India, which is what the app is in on an Indian phone
        val formatter = dateFormatter(Locale.forLanguageTag("en-IN"))

        assertEquals("4:20 pm", formatter.formatTime(millis(2026, Calendar.OCTOBER, 11, hour = 16) + 20 * 60_000))
        assertEquals("9:00 am", formatter.formatTime(millis(2026, Calendar.OCTOBER, 11, hour = 9)))
        assertEquals("12:00 am", formatter.formatTime(millis(2026, Calendar.OCTOBER, 11, hour = 0)))
    }

    @Test
    fun formatDateTime_isDayMonthYearThenTheTime() {
        val formatter = dateFormatter(Locale.forLanguageTag("en-IN"))

        assertEquals(
            "11 Oct 2026, 4:20 pm",
            formatter.formatDateTime(millis(2026, Calendar.OCTOBER, 11, hour = 16) + 20 * 60_000)
        )
    }

    @Test
    fun formatMonthRange_repeatsOnlyWhatDiffers() {
        val jan = millis(2026, Calendar.JANUARY, 1)
        val oct = millis(2026, Calendar.OCTOBER, 11)

        assertEquals("Jan – Oct 2026", formatter.formatMonthRange(jan, oct))
        assertEquals("Oct 2026", formatter.formatMonthRange(millis(2026, Calendar.OCTOBER, 1), oct))
        assertEquals("Oct 2026", formatter.formatMonthRange(oct, oct))
        assertEquals("Nov 2025 – Oct 2026", formatter.formatMonthRange(millis(2025, Calendar.NOVEMBER, 30), oct))
        assertEquals("Oct 2025 – Oct 2026", formatter.formatMonthRange(millis(2025, Calendar.OCTOBER, 11), oct))
    }

    @Test
    fun formatIsoDate_isYearMonthDay_zeroPadded() {
        assertEquals("2026-10-08", formatter.formatIsoDate(millis(2026, Calendar.OCTOBER, 8)))
        assertEquals("2027-01-03", formatter.formatIsoDate(millis(2027, Calendar.JANUARY, 3)))
    }

    @Test
    fun formatIsoDate_usesWesternDigits_whateverTheLanguage() {
        val day = millis(2026, Calendar.OCTOBER, 8)

        listOf("ar-EG", "hi-IN-u-nu-deva", "fa-IR").forEach { tag ->
            Locale.setDefault(Locale.forLanguageTag(tag))

            assertEquals("2026-10-08", formatter.formatIsoDate(day))
        }
    }

    // --- The app in Hindi. The exact spellings come from the platform's locale data, so the
    // tests check the script and the digits rather than one spelling.

    private val devanagari = Regex("[\\u0900-\\u097F]")
    private val nonWesternDigit = Regex("[\\u0966-\\u096F]")

    @Test
    fun inHindi_monthAndDayNamesAreHindi_whateverThePhonesLanguage() {
        // Saturday 17 Oct 2026
        val day = millis(2026, Calendar.OCTOBER, 17)

        listOf(
            hindiFormatter.formatDate(day),
            hindiFormatter.formatMonthYear(day),
            hindiFormatter.formatShortMonth(day),
            hindiFormatter.formatWeekday(day),
            hindiFormatter.formatDayHeader(day),
            hindiFormatter.formatDayRange(day, day, withYear = true)
        ).forEach { text ->
            assertTrue("$text should be in Devanagari", devanagari.containsMatchIn(text))
            assertFalse("$text should have no Latin letters", text.any { it in 'A'..'Z' || it in 'a'..'z' })
        }
    }

    @Test
    fun inHindi_digitsStayWestern() {
        val day = millis(2026, Calendar.OCTOBER, 17, hour = 16)

        assertEquals("2026", hindiFormatter.formatYear(day))
        assertEquals("17", hindiFormatter.formatDayOfMonth(day))
        listOf(
            hindiFormatter.formatDate(day),
            hindiFormatter.formatDateTime(day),
            hindiFormatter.formatDayRange(millis(2026, Calendar.OCTOBER, 1), day, withYear = true)
        ).forEach { text ->
            assertFalse("$text should have Western digits", nonWesternDigit.containsMatchIn(text))
        }
        assertTrue(hindiFormatter.formatDate(day).startsWith("17 "))
        assertTrue(hindiFormatter.formatDate(day).endsWith(", 2026"))
    }

    @Test
    fun inHindi_theMonthInitialKeepsItsVowelSign() {
        val july = hindiFormatter.formatShortMonth(millis(2026, Calendar.JULY, 10))
        val initial = hindiFormatter.formatMonthInitial(millis(2026, Calendar.JULY, 10))

        assertTrue(july.startsWith(initial))
        // जु, not a bare ज: the sign written onto the letter belongs to it
        assertEquals("जु", initial)
    }

    @Test
    fun formatIsoDate_isTheSameInEveryLanguage() {
        val day = millis(2026, Calendar.OCTOBER, 8)

        assertEquals("2026-10-08", formatter.formatIsoDate(day))
        assertEquals("2026-10-08", hindiFormatter.formatIsoDate(day))
    }

    @Test
    fun followsTheAppsLanguage_whenItChanges() {
        val localeProvider = FakeAppLocaleProvider(Locale.US)
        val following = DateFormatter(localeProvider, FakeTimeProvider(nowMillis = 0L))
        val day = millis(2026, Calendar.OCTOBER, 8)

        assertEquals("October 2026", following.formatMonthYear(day))

        localeProvider.locale = HINDI

        assertEquals(hindiFormatter.formatMonthYear(day), following.formatMonthYear(day))
        assertTrue(devanagari.containsMatchIn(following.formatMonthYear(day)))
    }
}
