package com.example.expensetrackerpro.domain.currency

/** How the whole part of an amount is split into groups of digits. */
enum class DigitGrouping {
    /** Last three digits, then groups of two: `12,34,567`. */
    INDIAN,

    /** Groups of three: `1,234,567`. */
    THOUSANDS
}

/**
 * A currency the app can show amounts in. [code] is its ISO 4217 code. [fractionDigits] is how many
 * decimals an amount is written with: 2, or 0 for a currency without a minor unit in use. Money
 * always holds hundredths of the unit, so no currency has more than 2.
 */
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val fractionDigits: Int,
    val grouping: DigitGrouping
) {
    INR(code = "INR", symbol = "₹", fractionDigits = 2, grouping = DigitGrouping.INDIAN)
}
