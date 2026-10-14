package com.example.expensetrackerpro.utils

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import com.example.expensetrackerpro.domain.currency.DigitGrouping
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.models.Money
import javax.inject.Inject
import kotlin.math.abs

/**
 * The single place money is turned into text, in the currency [CurrencyProvider] gives: its
 * symbol, its digit grouping and its decimals. For rupees, `₹1,00,000.00` or `-₹250.50`. Digits
 * are always Western. Composables reach it through `LocalCurrencyFormatter`.
 *
 * [localeProvider] is not read yet: rupees are written the same in every language the app has.
 * It is here for currencies whose symbol goes after the number in some languages.
 */
class CurrencyFormatter @Inject constructor(
    private val currencyProvider: CurrencyProvider,
    private val localeProvider: AppLocaleProvider
) {

    /** The current currency's symbol, e.g. `₹`. */
    val symbol: String get() = currencyProvider.current().symbol

    /** What [groupInput] puts between groups of digits. */
    val groupingSeparator: Char get() = GROUPING_SEPARATOR

    fun format(amount: Money): String {
        val currency = currencyProvider.current()
        if (currency.fractionDigits == 0) return formatWhole(amount)

        val minorUnits = abs(amount.minorUnits)
        val units = minorUnits / Money.MINOR_PER_UNIT
        val decimals = (minorUnits % Money.MINOR_PER_UNIT).toString().padStart(2, '0')
        val sign = if (amount.isNegative) "-" else ""

        return "$sign${currency.symbol}${group(units.toString(), currency)}.$decimals"
    }

    /** Always signed: `+₹500.00` for income, `-₹500.00` for an expense. */
    fun formatSigned(amount: Money, isIncome: Boolean): String =
        (if (isIncome) "+" else "-") + format(amount.abs())

    /** Rounded to whole units, for summaries where the decimals are noise: `₹27,550`. */
    fun formatWhole(amount: Money): String {
        val currency = currencyProvider.current()
        val units = wholeUnits(amount)
        val sign = if (amount.isNegative && units != 0L) "-" else ""

        return "$sign${currency.symbol}${group(units.toString(), currency)}"
    }

    /**
     * At most one decimal with a size suffix, for chart axes. Rupees use k, L (lakh) and
     * Cr (crore): `₹950`, `₹2.4k`, `₹12k`, `₹1.5L`, `₹2Cr`.
     */
    fun formatShort(amount: Money): String {
        val currency = currencyProvider.current()
        val shortUnits = shortUnitsOf(currency.grouping)
        val units = wholeUnits(amount)
        val sign = if (amount.isNegative && units != 0L) "-" else ""

        var unitIndex = shortUnits.indexOfFirst { (size, _) -> units >= size }
        if (unitIndex == -1) return "$sign${currency.symbol}$units"

        // Tenths of the size, halves rounded up
        var tenths = (units * 10 + shortUnits[unitIndex].first / 2) / shortUnits[unitIndex].first
        // Rounding can reach the next size up: 99,960 is 1L, not 100k
        if (unitIndex > 0 && tenths * shortUnits[unitIndex].first >= shortUnits[unitIndex - 1].first * 10) {
            unitIndex -= 1
            tenths = 10
        }

        val whole = tenths / 10
        val decimal = tenths % 10
        val number = if (decimal == 0L) "$whole" else "$whole.$decimal"

        return "$sign${currency.symbol}$number${shortUnits[unitIndex].second}"
    }

    /**
     * An amount as it is being typed, with only grouping added to its whole part:
     * `123456.7` becomes `1,23,456.7`. No symbol, and the decimals are left as typed.
     */
    fun groupInput(input: String): String {
        val integerPart = input.substringBefore('.')

        return group(integerPart, currencyProvider.current()) + input.removePrefix(integerPart)
    }

    // The size of [amount] in whole units, to the nearest one with halves rounded up
    private fun wholeUnits(amount: Money): Long = abs(amount.roundedToRupees().minorUnits) / Money.MINOR_PER_UNIT

    private fun group(digits: String, currency: AppCurrency): String = when (currency.grouping) {
        DigitGrouping.INDIAN -> groupIndian(digits)
        DigitGrouping.THOUSANDS -> groupThousands(digits)
    }

    private companion object {
        const val GROUPING_SEPARATOR = ','

        // Largest first: crore, lakh, thousand
        val INDIAN_SHORT_UNITS = listOf(10_000_000L to "Cr", 100_000L to "L", 1_000L to "k")

        // Largest first: billion, million, thousand
        val THOUSANDS_SHORT_UNITS = listOf(1_000_000_000L to "B", 1_000_000L to "M", 1_000L to "k")

        fun shortUnitsOf(grouping: DigitGrouping): List<Pair<Long, String>> = when (grouping) {
            DigitGrouping.INDIAN -> INDIAN_SHORT_UNITS
            DigitGrouping.THOUSANDS -> THOUSANDS_SHORT_UNITS
        }

        // Last three digits form one group, then groups of two: 12,34,567
        fun groupIndian(digits: String): String {
            if (digits.length <= 3) return digits

            val lastThree = digits.takeLast(3)
            val leading = digits.dropLast(3)
                .reversed()
                .chunked(2)
                .joinToString(GROUPING_SEPARATOR.toString())
                .reversed()

            return "$leading$GROUPING_SEPARATOR$lastThree"
        }

        // Groups of three: 1,234,567
        fun groupThousands(digits: String): String =
            digits.reversed()
                .chunked(3)
                .joinToString(GROUPING_SEPARATOR.toString())
                .reversed()
    }
}
