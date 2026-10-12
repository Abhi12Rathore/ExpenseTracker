package com.example.expensetrackerpro.domain.usecase.scan

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Finds the bill's date: the first on it, written as `dd/MM/yyyy`, `dd-MM-yy`, `dd.MM.yyyy`,
 * `dd MMM yyyy` or `yyyy-MM-dd`, that is neither in the future nor more than a year back. A date
 * that reads both ways, such as 05/10, is taken day first.
 */
internal class BillDateParser(keywords: BillKeywords) {

    private val monthNames = keywords.months

    // 11 Oct 2026, 11-Oct-26, 11 October 2026
    private val namedMonth = Regex(
        """(?<!\d)(\d{1,2})[\s.,/-]*(${monthNames.joinToString("|", transform = Regex::escape)})\p{L}*[\s.,/-]*(\d{4}|\d{2})(?!\d)""",
        RegexOption.IGNORE_CASE
    )

    private val patterns = listOf(YEAR_FIRST, DAY_FIRST, namedMonth)

    /** Midnight UTC of the date, as a date picker reports one, or null when there is none to trust. */
    fun parse(lines: List<String>, today: Calendar): Long? {
        val latest = BillDate(today)
        val earliest = BillDate((today.clone() as Calendar).apply { add(Calendar.YEAR, -1) })

        return lines.asSequence()
            .flatMap(::datesIn)
            .firstOrNull { it >= earliest && it <= latest }
            ?.utcMillis()
    }

    /** [line] without its dates, whose digits would otherwise read as amounts. */
    fun strip(line: String): String = patterns.fold(line) { text, pattern -> pattern.replace(text, " ") }

    private fun datesIn(line: String): Sequence<BillDate> =
        // Group 2 of the numeric patterns is the separator
        YEAR_FIRST.findAll(line).mapNotNull { match ->
            val (year, _, month, day) = match.destructured
            dateOf(year.toInt(), month.toInt(), day.toInt())
        } + DAY_FIRST.findAll(line).mapNotNull { match ->
            val (first, _, second, year) = match.destructured
            dayFirst(first.toInt(), second.toInt(), fullYear(year))
        } + namedMonth.findAll(line).mapNotNull { match ->
            val (day, month, year) = match.destructured
            dateOf(fullYear(year), monthNames.indexOf(month.lowercase()) + 1, day.toInt())
        }

    /** Day first, unless only the other way round is a date at all: 09/25 is 25 September. */
    private fun dayFirst(first: Int, second: Int, year: Int): BillDate? =
        dateOf(year, month = second, day = first) ?: dateOf(year, month = first, day = second)

    private fun fullYear(year: String): Int = if (year.length == 2) CENTURY + year.toInt() else year.toInt()

    private fun dateOf(year: Int, month: Int, day: Int): BillDate? {
        if (month !in 1..12 || day < 1) return null
        val daysInMonth = GregorianCalendar(year, month - 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH)

        return if (day <= daysInMonth) BillDate(year, month, day) else null
    }

    /** A calendar day, with [month] from 1. */
    private data class BillDate(val year: Int, val month: Int, val day: Int) : Comparable<BillDate> {

        constructor(calendar: Calendar) : this(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        override fun compareTo(other: BillDate): Int =
            compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

        fun utcMillis(): Long = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis
    }

    private companion object {
        const val CENTURY = 2000

        // The same separator twice, so that "12.50 - 3.75" is not a date
        val YEAR_FIRST = Regex("""(?<!\d)(\d{4})([/.-])(\d{1,2})\2(\d{1,2})(?!\d)""")
        val DAY_FIRST = Regex("""(?<!\d)(\d{1,2})([/.-])(\d{1,2})\2(\d{4}|\d{2})(?!\d)""")
    }
}
