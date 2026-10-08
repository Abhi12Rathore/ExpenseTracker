package com.example.expensetrackerpro.utils

import java.util.Locale
import kotlin.math.abs

/**
 * The single place money is turned into text: rupee symbol, Indian digit
 * grouping and two decimals, e.g. `₹1,00,000.00` or `-₹250.50`.
 */
object CurrencyFormatter {

    private const val SYMBOL = "₹"

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
