package com.example.expensetrackerpro.domain.usecase.quickadd

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Finds the day a sentence speaks of: a word ("yesterday", "kal", "parso"), a weekday ("last
 * monday"), a day and month ("12 oct", "oct 12th") or numbers, day first ("12/10", "12-10-26").
 * A day without a year that is months ahead is taken from last year. The first date in the
 * sentence wins; times of day ("8pm", "9:30") are only marked, so they aren't read as amounts.
 */
internal class QuickAddDateParser(private val keywords: QuickAddKeywords) {

    // Longest first, so "day before yesterday" isn't read as "yesterday"
    private val phrases = keywords.dayOffsets.entries
        .map { (phrase, offset) -> phrase.split(' ') to offset }
        .sortedByDescending { (words, _) -> words.size }

    /** Marks the tokens that are a time of day as [used]. */
    fun markTimes(tokens: List<Token>, used: BooleanArray) {
        tokens.forEachIndexed { i, token ->
            if (used[i] || token.kind != Token.Kind.NUMBER) return@forEachIndexed
            val parts = QuickAddTokens.numberParts(token) ?: return@forEachIndexed

            when {
                // 8pm
                parts.suffix in keywords.timeSuffixes -> used[i] = true
                // 9 am
                parts.suffix.isEmpty() && tokens.wordAt(i + 1) in keywords.timeSuffixes -> {
                    used[i] = true
                    used[i + 1] = true
                }
                // 9:30, 9:30pm
                parts.suffix.isEmpty() && tokens.getOrNull(i + 1)?.text == ":" &&
                    tokens.getOrNull(i + 2)?.kind == Token.Kind.NUMBER -> {
                    used[i] = true
                    used[i + 1] = true
                    used[i + 2] = true
                    if (tokens.wordAt(i + 3) in keywords.timeSuffixes) used[i + 3] = true
                }
            }
        }
    }

    /** Midnight UTC of the day found, with its tokens marked as [used]; null when there is none. */
    fun parse(tokens: List<Token>, used: BooleanArray, today: Calendar): Long? {
        for (i in tokens.indices) {
            if (used[i]) continue
            val found = numericDate(tokens, used, i, today)
                ?: namedMonthDate(tokens, used, i, today)
                ?: dayOffset(tokens, used, i, today)
                ?: weekday(tokens, used, i, today)
                ?: continue

            found.span.forEach { used[it] = true }
            return found.day.utcMillis()
        }
        return null
    }

    /** 12/10, 12-10, 12/10/2026, 12-10-26 */
    private fun numericDate(tokens: List<Token>, used: BooleanArray, i: Int, today: Calendar): Found? {
        val day = tokens.plainInt(i, used) ?: return null
        val separator = tokens.getOrNull(i + 1)?.text?.takeIf { it in DATE_SEPARATORS } ?: return null
        val month = tokens.plainInt(i + 2, used) ?: return null

        val year = if (tokens.getOrNull(i + 3)?.text == separator) tokens.plainInt(i + 4, used) else null
        val span = if (year != null) i..i + 4 else i..i + 2
        val date = if (year != null) {
            Day.of(fullYear(year), month, day)
        } else {
            Day.withoutYear(month, day, today)
        }

        return date?.let { Found(it, span) }
    }

    /** 12 oct, 12th october 2025, oct 12 */
    private fun namedMonthDate(tokens: List<Token>, used: BooleanArray, i: Int, today: Calendar): Found? {
        dayOfMonth(tokens, used, i)?.let { day ->
            val month = monthAt(tokens, used, i + 1) ?: return@let
            return withOptionalYear(tokens, used, i, i + 1, month, day, today)
        }

        val month = monthAt(tokens, used, i) ?: return null
        val day = dayOfMonth(tokens, used, i + 1) ?: return null
        return withOptionalYear(tokens, used, i, i + 1, month, day, today)
    }

    private fun withOptionalYear(
        tokens: List<Token>,
        used: BooleanArray,
        start: Int,
        last: Int,
        month: Int,
        day: Int,
        today: Calendar
    ): Found? {
        val year = tokens.plainInt(last + 1, used)?.takeIf { it >= MIN_FULL_YEAR }
        val date = if (year != null) Day.of(year, month, day) else Day.withoutYear(month, day, today)

        return date?.let { Found(it, start..if (year != null) last + 1 else last) }
    }

    /** today, yesterday, kal, parso, day before yesterday */
    private fun dayOffset(tokens: List<Token>, used: BooleanArray, i: Int, today: Calendar): Found? {
        for ((words, offset) in phrases) {
            val matches = words.indices.all { k -> !used.isUsed(i + k) && tokens.wordAt(i + k) == words[k] }
            if (matches) {
                val calendar = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
                return Found(Day(calendar), i until i + words.size)
            }
        }
        return null
    }

    /** last monday, monday */
    private fun weekday(tokens: List<Token>, used: BooleanArray, i: Int, today: Calendar): Found? {
        val isLast = tokens.wordAt(i) in keywords.lastWords
        val at = if (isLast) i + 1 else i
        if (used.isUsed(at)) return null
        val word = tokens.wordAt(at) ?: return null
        val index = keywords.weekdays.indexOfFirst { word in it }.takeIf { it >= 0 } ?: return null

        // Calendar counts weekdays from Sunday = 1, as the list does from 0
        val back = (today.get(Calendar.DAY_OF_WEEK) - (index + 1) + DAYS_IN_WEEK) % DAYS_IN_WEEK
        // "last monday" said on a Monday is a week ago; "monday" said on a Monday is today
        val days = if (back == 0 && isLast) DAYS_IN_WEEK else back
        val calendar = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -days) }

        return Found(Day(calendar), i..at)
    }

    private fun dayOfMonth(tokens: List<Token>, used: BooleanArray, i: Int): Int? {
        if (used.isUsed(i)) return null
        val token = tokens.getOrNull(i)?.takeIf { it.kind == Token.Kind.NUMBER } ?: return null
        val parts = QuickAddTokens.numberParts(token) ?: return null
        if (parts.suffix.isNotEmpty() && parts.suffix !in keywords.ordinalSuffixes) return null

        return parts.digits.toIntOrNull()?.takeIf { it in 1..MAX_DAY }
    }

    /** The month from 1, if the word at [i] is one. */
    private fun monthAt(tokens: List<Token>, used: BooleanArray, i: Int): Int? {
        if (used.isUsed(i)) return null
        val word = tokens.wordAt(i) ?: return null
        val index = keywords.months.indexOfFirst { word in it }

        return if (index >= 0) index + 1 else null
    }

    /** A plain number at [i], with no decimals, commas or letters. */
    private fun List<Token>.plainInt(i: Int, used: BooleanArray): Int? {
        if (used.isUsed(i)) return null
        val token = getOrNull(i)?.takeIf { it.kind == Token.Kind.NUMBER } ?: return null
        return token.text.takeIf { text -> text.all(Char::isDigit) && text.length <= YEAR_DIGITS }?.toInt()
    }

    private fun fullYear(year: Int): Int = if (year < CENTURY) CENTURY + year else year

    private data class Found(val day: Day, val span: IntRange)

    /** A calendar day, with [month] from 1. */
    private data class Day(val year: Int, val month: Int, val day: Int) : Comparable<Day> {

        constructor(calendar: Calendar) : this(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        override fun compareTo(other: Day): Int =
            compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

        fun utcMillis(): Long = GregorianCalendar(UTC).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis

        companion object {
            fun of(year: Int, month: Int, day: Int): Day? {
                if (month !in 1..MONTHS || day < 1) return null
                val daysInMonth = GregorianCalendar(year, month - 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH)
                return if (day <= daysInMonth) Day(year, month, day) else null
            }

            /**
             * This year's, or last year's when this year's is months away: "12 dec" said in
             * January. One only days away is kept, in the future, so that it is dropped as a slip
             * rather than turned into a date a year back.
             */
            fun withoutYear(month: Int, day: Int, today: Calendar): Day? {
                val now = Day(today)
                val thisYear = of(now.year, month, day) ?: return of(now.year - 1, month, day)
                val daysAhead = (thisYear.utcMillis() - now.utcMillis()) / MILLIS_PER_DAY

                return if (daysAhead > NEAR_FUTURE_DAYS) of(now.year - 1, month, day) else thisYear
            }
        }
    }

    private companion object {
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
        val DATE_SEPARATORS = setOf("/", "-")
        const val DAYS_IN_WEEK = 7
        const val MONTHS = 12
        const val MAX_DAY = 31
        const val YEAR_DIGITS = 4
        const val CENTURY = 2000
        const val MIN_FULL_YEAR = 1900
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
        const val NEAR_FUTURE_DAYS = 31
    }
}

/** The lowercase word at [i], or null when there is none or it is not a word. */
internal fun List<Token>.wordAt(i: Int): String? = getOrNull(i)?.takeIf { it.kind == Token.Kind.WORD }?.lower

/** Whether [i] is outside the sentence or already taken by another value. */
internal fun BooleanArray.isUsed(i: Int): Boolean = i !in indices || this[i]
