package com.example.expensetrackerpro.utils

import java.util.Locale
import kotlin.math.abs

/**
 * The single place money is turned into text: rupee symbol, Indian digit
 * grouping and two decimals, e.g. `₹1,00,000.00` or `-₹250.50`.
 */
object CurrencyFormatter {

    const val SYMBOL = "₹"

    // Largest first: crore, lakh, thousand
    private val SHORT_UNITS = listOf(10_000_000L to "Cr", 100_000L to "L", 1_000L to "k")

    fun format(amount: Double): String {
        val fixed = String.format(Locale.US, "%.2f", abs(amount))
        val integerPart = fixed.substringBefore('.')
        val decimals = fixed.substringAfter('.')
        // A negative that rounds to 0.00 is shown without a sign
        val sign = if (amount < 0 && fixed != "0.00") "-" else ""

        return "$sign$SYMBOL${groupIndian(integerPart)}.$decimals"
    }

    /** Always signed: `+₹500.00` for income, `-₹500.00` for an expense. */
    fun formatSigned(amount: Double, isIncome: Boolean): String =
        (if (isIncome) "+" else "-") + format(abs(amount))

    /** Rounded to whole rupees, for summaries where paise are noise: `₹27,550`. */
    fun formatWhole(amount: Double): String {
        val rupees = Math.round(abs(amount))
        val sign = if (amount < 0 && rupees != 0L) "-" else ""

        return "$sign$SYMBOL${groupIndian(rupees.toString())}"
    }

    /**
     * At most one decimal with a k, L (lakh) or Cr (crore) suffix, for chart axes:
     * `₹950`, `₹2.4k`, `₹12k`, `₹1.5L`, `₹2Cr`.
     */
    fun formatShort(amount: Double): String {
        val rupees = Math.round(abs(amount))
        val sign = if (amount < 0 && rupees != 0L) "-" else ""

        var unitIndex = SHORT_UNITS.indexOfFirst { (size, _) -> rupees >= size }
        if (unitIndex == -1) return "$sign$SYMBOL$rupees"

        var tenths = Math.round(rupees * 10.0 / SHORT_UNITS[unitIndex].first)
        // Rounding can reach the next unit up: 99,960 is 1L, not 100k
        if (unitIndex > 0 && tenths * SHORT_UNITS[unitIndex].first >= SHORT_UNITS[unitIndex - 1].first * 10) {
            unitIndex -= 1
            tenths = 10
        }

        val whole = tenths / 10
        val decimal = tenths % 10
        val number = if (decimal == 0L) "$whole" else "$whole.$decimal"

        return "$sign$SYMBOL$number${SHORT_UNITS[unitIndex].second}"
    }

    /**
     * An amount as it is being typed, with only grouping added to its whole part:
     * `123456.7` becomes `1,23,456.7`. No symbol, and the decimals are left as typed.
     */
    fun groupInput(input: String): String {
        val integerPart = input.substringBefore('.')

        return groupIndian(integerPart) + input.removePrefix(integerPart)
    }

    // Last three digits form one group, then groups of two: 12,34,567
    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits

        val lastThree = digits.takeLast(3)
        val leading = digits.dropLast(3)
            .reversed()
            .chunked(2)
            .joinToString(",")
            .reversed()

        return "$leading,$lastThree"
    }
}
