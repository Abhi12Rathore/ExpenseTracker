package com.example.expensetrackerpro.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** The single place epoch millis are turned into text for the UI. */
object DateFormatter {

    /** e.g. `08 Oct, 2026` */
    fun formatDate(millis: Long): String = format("dd MMM, yyyy", millis)

    /** e.g. `October 2026` */
    fun formatMonthYear(millis: Long): String = format("MMMM yyyy", millis)

    /** e.g. `2026` */
    fun formatYear(millis: Long): String = format("yyyy", millis)

    /** e.g. `Oct 2026` */
    fun formatShortMonthYear(millis: Long): String = format("MMM yyyy", millis)

    /** e.g. `Oct` */
    fun formatShortMonth(millis: Long): String = format("MMM", millis)

    /** The month's first letter, e.g. `O`, for tight chart axes. */
    fun formatMonthInitial(millis: Long): String = formatShortMonth(millis).take(1)

    /** e.g. `Fri` */
    fun formatWeekday(millis: Long): String = format("EEE", millis)

    /** Day of the month without padding, e.g. `8` */
    fun formatDayOfMonth(millis: Long): String = format("d", millis)

    /** e.g. `9 Oct` */
    fun formatDayMonth(millis: Long): String = format("d MMM", millis)

    /** e.g. `Fri, 17 Oct` */
    fun formatWeekdayDayMonth(millis: Long): String = format("EEE, d MMM", millis)

    /** e.g. `Fri 9 Oct`, for the header of a day's transactions */
    fun formatDayHeader(millis: Long): String = format("EEE d MMM", millis)

    /** The time of day on a 12-hour clock, e.g. `4:20 pm` */
    fun formatTime(millis: Long): String = format("h:mm a", millis)

    /** e.g. `11 Oct 2026, 4:20 pm` */
    fun formatDateTime(millis: Long): String = format("d MMM yyyy, h:mm a", millis)

    /**
     * The months two dates span, both inclusive, repeating only what differs: `Jan – Oct 2026`,
     * `Oct 2026` for a single month, `Nov 2025 – Oct 2026` across a year.
     */
    fun formatMonthRange(startMillis: Long, endMillis: Long): String {
        val start = Calendar.getInstance().apply { timeInMillis = startMillis }
        val end = Calendar.getInstance().apply { timeInMillis = endMillis }
        val sameYear = start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
        val sameMonth = sameYear && start.get(Calendar.MONTH) == end.get(Calendar.MONTH)

        return when {
            sameMonth -> formatShortMonthYear(endMillis)
            sameYear -> "${formatShortMonth(startMillis)} – ${formatShortMonthYear(endMillis)}"
            else -> "${formatShortMonthYear(startMillis)} – ${formatShortMonthYear(endMillis)}"
        }
    }

    /**
     * ISO 8601, e.g. `2026-10-08`, for files and file names. Always in Western digits,
     * whatever the device's language.
     */
    fun formatIsoDate(millis: Long): String = format("yyyy-MM-dd", millis, Locale.US)

    /**
     * Both days inclusive, repeating only what differs: `1 – 31 Oct`, `28 Sep – 4 Oct`, or
     * `8 Oct` for a single day. [withYear] appends the year (`1 – 31 Oct 2026`); a range that
     * crosses a year always shows both (`29 Dec 2025 – 4 Jan 2026`).
     */
    fun formatDayRange(startMillis: Long, endMillis: Long, withYear: Boolean): String {
        val start = Calendar.getInstance().apply { timeInMillis = startMillis }
        val end = Calendar.getInstance().apply { timeInMillis = endMillis }
        val sameYear = start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
        val sameMonth = sameYear && start.get(Calendar.MONTH) == end.get(Calendar.MONTH)
        val sameDay = sameMonth && start.get(Calendar.DAY_OF_MONTH) == end.get(Calendar.DAY_OF_MONTH)

        if (!sameYear) {
            return "${format("d MMM yyyy", startMillis)} – ${format("d MMM yyyy", endMillis)}"
        }

        val days = when {
            sameDay -> format("d MMM", endMillis)
            sameMonth -> "${format("d", startMillis)} – ${format("d MMM", endMillis)}"
            else -> "${format("d MMM", startMillis)} – ${format("d MMM", endMillis)}"
        }
        return if (withYear) "$days ${formatYear(endMillis)}" else days
    }

    // SimpleDateFormat is not thread-safe, so a new one is created per call
    private fun format(pattern: String, millis: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat(pattern, locale).format(Date(millis))
}
