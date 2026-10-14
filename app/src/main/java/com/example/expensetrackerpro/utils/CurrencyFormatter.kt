package com.example.expensetrackerpro.utils

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import com.example.expensetrackerpro.domain.currency.DigitGrouping
import com.example.expensetrackerpro.domain.currency.NumberSymbols
import com.example.expensetrackerpro.domain.currency.SymbolPlacement
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.locale.NumberSymbolsProvider
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.Money
import javax.inject.Inject
import kotlin.math.abs

/**
 * The single place money is turned into text. The currency (from [CurrencyProvider], unless one is
 * passed) decides the symbol, the digit grouping (Indian for rupees only) and the decimals (none
 * for yen). The app's language decides the separators and where the symbol goes:
 * `₹1,00,000.50`, `$100,000.50`, `100.000,50 €`, `R$ 100.000,50`. Digits are always Western.
 * Composables reach it through `LocalCurrencyFormatter`.
 */
class CurrencyFormatter @Inject constructor(
    private val currencyProvider: CurrencyProvider,
    private val localeProvider: AppLocaleProvider,
    private val numberSymbolsProvider: NumberSymbolsProvider
) {

    /** The current currency's symbol, e.g. `₹`. */
    val symbol: String get() = currencyProvider.current().symbol

    /** Whether the app's language writes the symbol after the amount, as in `5,00 €`. */
    val isSymbolAfter: Boolean get() = language().symbolPlacement == SymbolPlacement.AFTER

    /** What [groupInput] puts between groups of digits. */
    val groupingSeparator: Char get() = numberSymbols().groupingSeparator

    /** What [groupInput] writes for the decimal point. */
    val decimalSeparator: Char get() = numberSymbols().decimalSeparator

    fun format(amount: Money, currency: AppCurrency = currencyProvider.current()): String {
        if (currency.fractionDigits == 0) return formatWhole(amount, currency)

        val symbols = numberSymbols()
        val minorUnits = abs(amount.minorUnits)
        val units = minorUnits / Money.MINOR_PER_UNIT
        val decimals = (minorUnits % Money.MINOR_PER_UNIT).toString().padStart(2, '0')
        val number = "${group(units.toString(), currency, symbols)}${symbols.decimalSeparator}$decimals"

        return withSymbol(number, isNegative = amount.isNegative, currency)
    }

    /** Always signed: `+₹500.00` for income, `-₹500.00` for an expense. */
    fun formatSigned(amount: Money, isIncome: Boolean, currency: AppCurrency = currencyProvider.current()): String =
        (if (isIncome) "+" else "-") + format(amount.abs(), currency)

    /** Rounded to whole units, for summaries where the decimals are noise: `₹27,550`. */
    fun formatWhole(amount: Money, currency: AppCurrency = currencyProvider.current()): String {
        val units = wholeUnits(amount)
        val number = group(units.toString(), currency, numberSymbols())

        return withSymbol(number, isNegative = amount.isNegative && units != 0L, currency)
    }

    /**
     * At most one decimal with a size suffix, for chart axes. Rupees use k, L (lakh) and
     * Cr (crore): `₹950`, `₹2.4k`, `₹12k`, `₹1.5L`, `₹2Cr`. Other currencies use K, M and B:
     * `$12K`, `1,5M €`.
     */
    fun formatShort(amount: Money, currency: AppCurrency = currencyProvider.current()): String {
        val shortUnits = shortUnitsOf(currency.grouping)
        val units = wholeUnits(amount)
        val isNegative = amount.isNegative && units != 0L

        var unitIndex = shortUnits.indexOfFirst { (size, _) -> units >= size }
        if (unitIndex == -1) return withSymbol(units.toString(), isNegative, currency)

        // Tenths of the size, halves rounded up
        var tenths = (units * 10 + shortUnits[unitIndex].first / 2) / shortUnits[unitIndex].first
        // Rounding can reach the next size up: 99,960 is 1L, not 100k
        if (unitIndex > 0 && tenths * shortUnits[unitIndex].first >= shortUnits[unitIndex - 1].first * 10) {
            unitIndex -= 1
            tenths = 10
        }

        val whole = tenths / 10
        val decimal = tenths % 10
        val number = if (decimal == 0L) "$whole" else "$whole${numberSymbols().decimalSeparator}$decimal"

        return withSymbol("$number${shortUnits[unitIndex].second}", isNegative, currency)
    }

    /**
     * An amount as it is being typed, kept as plain digits with `.` for the decimal point, as it
     * is shown: grouping added to its whole part and the language's decimal separator.
     * `123456.7` becomes `1,23,456.7` for rupees in English, `123.456,7` for euros in German.
     * No symbol, and the decimals are left as typed.
     */
    fun groupInput(input: String): String {
        val symbols = numberSymbols()
        val integerPart = input.substringBefore('.')
        val rest = input.removePrefix(integerPart).replace('.', symbols.decimalSeparator)

        return group(integerPart, currencyProvider.current(), symbols) + rest
    }

    private fun numberSymbols(): NumberSymbols = numberSymbolsProvider.symbolsFor(localeProvider.current())

    // A language the app isn't translated into is shown in English, and writes amounts as English does
    private fun language(): AppLanguage =
        AppLanguage.findByTag(localeProvider.current().toLanguageTag()) ?: AppLanguage.ENGLISH

    // The sign goes first, wherever the language puts the symbol: -$5.00, -5,00 €
    private fun withSymbol(number: String, isNegative: Boolean, currency: AppCurrency): String {
        val language = language()
        val sign = if (isNegative) "-" else ""

        return when (language.symbolPlacement) {
            SymbolPlacement.AFTER -> "$sign$number$SPACE${currency.symbol}"
            SymbolPlacement.BEFORE -> {
                // A symbol made of letters needs the space to stay readable: AED 5.00, not AED5.00
                val space = if (language.spaceAfterSymbol || currency.symbol.last().isLetter()) SPACE else ""
                "$sign${currency.symbol}$space$number"
            }
        }
    }

    // The size of [amount] in whole units, to the nearest one with halves rounded up
    private fun wholeUnits(amount: Money): Long = abs(amount.roundedToRupees().minorUnits) / Money.MINOR_PER_UNIT

    private fun group(digits: String, currency: AppCurrency, symbols: NumberSymbols): String =
        when (currency.grouping) {
            DigitGrouping.INDIAN -> groupIndian(digits, symbols.groupingSeparator)
            DigitGrouping.THOUSANDS -> groupThousands(digits, symbols.groupingSeparator)
        }

    private companion object {
        // Keeps the symbol on the same line as its number
        const val SPACE = ' '

        // Largest first: crore, lakh, thousand
        val INDIAN_SHORT_UNITS = listOf(10_000_000L to "Cr", 100_000L to "L", 1_000L to "k")

        // Largest first: billion, million, thousand
        val THOUSANDS_SHORT_UNITS = listOf(1_000_000_000L to "B", 1_000_000L to "M", 1_000L to "K")

        fun shortUnitsOf(grouping: DigitGrouping): List<Pair<Long, String>> = when (grouping) {
            DigitGrouping.INDIAN -> INDIAN_SHORT_UNITS
            DigitGrouping.THOUSANDS -> THOUSANDS_SHORT_UNITS
        }

        // Last three digits form one group, then groups of two: 12,34,567
        fun groupIndian(digits: String, separator: Char): String {
            if (digits.length <= 3) return digits

            val lastThree = digits.takeLast(3)
            val leading = digits.dropLast(3)
                .reversed()
                .chunked(2)
                .joinToString(separator.toString())
                .reversed()

            return "$leading$separator$lastThree"
        }

        // Groups of three: 1,234,567
        fun groupThousands(digits: String, separator: Char): String =
            digits.reversed()
                .chunked(3)
                .joinToString(separator.toString())
                .reversed()
    }
}
