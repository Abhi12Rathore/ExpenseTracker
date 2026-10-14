package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.locale.NumberSymbolsProvider
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Filters the amount field as it is edited. The field holds plain digits with `.` for the decimal
 * point, whatever it shows. An edit is kept only while the text can still become a valid amount
 * (digits, one decimal point, two decimals, within the limit); anything else, such as pasted text,
 * leaves the field as it was.
 *
 * The decimal key is the language's: in a language that writes `12,50`, both `,` and `.` are taken
 * as the decimal point, as many keyboards only offer `.`; elsewhere a `,` is refused. A currency
 * written without decimals (yen) takes no decimal point, though an amount that already has
 * decimals keeps them, so saving it unchanged doesn't change it.
 */
class SanitizeAmountInputUseCase @Inject constructor(
    private val currencyProvider: CurrencyProvider,
    private val localeProvider: AppLocaleProvider,
    private val numberSymbolsProvider: NumberSymbolsProvider
) {

    operator fun invoke(previous: String, incoming: String): String {
        if (incoming.isEmpty()) return ""

        val decimalKey = numberSymbolsProvider.symbolsFor(localeProvider.current()).decimalSeparator
        val canonical = if (decimalKey == ',') incoming.replace(',', '.') else incoming

        val match = PARTIAL.matchEntire(canonical) ?: return previous
        val hasDecimalPoint = match.groupValues[2].isNotEmpty()
        if (hasDecimalPoint && '.' !in previous && currencyProvider.current().fractionDigits == 0) return previous

        // "007" reads as 7, and a bare "." as "0."
        val integerPart = match.groupValues[1].trimStart('0').ifEmpty { "0" }
        val sanitized = integerPart + match.groupValues[2]

        return if (BigDecimal(sanitized) > ValidateAmountUseCase.MAX_AMOUNT) previous else sanitized
    }

    private companion object {
        val PARTIAL = Regex("""([0-9]*)(\.[0-9]{0,2})?""")
    }
}
