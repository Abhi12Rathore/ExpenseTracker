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

    /** Hour of the day (0-23) in the device time zone. */
    fun hourOfDay(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

    // SimpleDateFormat is not thread-safe, so a new one is created per call
    private fun format(pattern: String, millis: Long): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}
