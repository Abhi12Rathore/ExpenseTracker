package com.example.expensetrackerpro.utils

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.fakes.FakeNumberSymbols.Companion.NARROW_NO_BREAK_SPACE
import com.example.expensetrackerpro.fakes.NBSP
import com.example.expensetrackerpro.fakes.currencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

/**
 * Every way an amount is written, for each currency in each language that writes it differently:
 * grouping from the currency (lakh only for rupees), separators and the symbol's place from the
 * language, and no decimals for yen. `_` stands for a no-break space and `·` for French's narrow one.
 */
@RunWith(Parameterized::class)
class CurrencyFormatterTableTest(
    private val currency: AppCurrency,
    private val languageTag: String,
    // 100,000.50 in full, whole and short; -1,130.00 in full; 1,500,000 short; 123456.7 as typed
    private val full: String,
    private val whole: String,
    private val short: String,
    private val negative: String,
    private val shortMillion: String,
    private val typed: String
) {

    private val formatter = currencyFormatter(currency, Locale.forLanguageTag(languageTag))

    @Test
    fun format() = assertEquals(full.spaces(), formatter.format(Money(100_000_50)))

    @Test
    fun formatWhole() = assertEquals(whole.spaces(), formatter.formatWhole(Money(100_000_50)))

    @Test
    fun formatShort() {
        assertEquals(short.spaces(), formatter.formatShort(Money(100_000_50)))
        assertEquals(shortMillion.spaces(), formatter.formatShort(Money.ofRupees(1_500_000)))
    }

    @Test
    fun negativeAmount_putsTheSignFirst() = assertEquals(negative.spaces(), formatter.format(Money(-1_130_00)))

    @Test
    fun formatSigned_isTheSignThenTheAmount() {
        assertEquals("+${full.spaces()}", formatter.formatSigned(Money(100_000_50), isIncome = true))
        assertEquals("-${full.spaces()}", formatter.formatSigned(Money(100_000_50), isIncome = false))
    }

    @Test
    fun groupInput() = assertEquals(typed.spaces(), formatter.groupInput("123456.7"))

    private fun String.spaces(): String = replace('_', NBSP).replace('·', NARROW_NO_BREAK_SPACE)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} in {1}")
        fun table(): List<Array<Any>> = listOf(
            row(AppCurrency.INR, "en", "₹1,00,000.50", "₹1,00,001", "₹1L", "-₹1,130.00", "₹15L", "1,23,456.7"),
            row(AppCurrency.INR, "hi", "₹1,00,000.50", "₹1,00,001", "₹1L", "-₹1,130.00", "₹15L", "1,23,456.7"),
            // Lakh grouping follows the currency, the separators the language
            row(AppCurrency.INR, "de", "1.00.000,50_₹", "1.00.001_₹", "1L_₹", "-1.130,00_₹", "15L_₹", "1.23.456,7"),
            row(AppCurrency.USD, "en", "$100,000.50", "$100,001", "$100K", "-$1,130.00", "$1.5M", "123,456.7"),
            row(AppCurrency.USD, "hi", "$100,000.50", "$100,001", "$100K", "-$1,130.00", "$1.5M", "123,456.7"),
            row(AppCurrency.EUR, "de", "100.000,50_€", "100.001_€", "100K_€", "-1.130,00_€", "1,5M_€", "123.456,7"),
            row(AppCurrency.EUR, "fr", "100·000,50_€", "100·001_€", "100K_€", "-1·130,00_€", "1,5M_€", "123·456,7"),
            row(AppCurrency.EUR, "es", "100.000,50_€", "100.001_€", "100K_€", "-1.130,00_€", "1,5M_€", "123.456,7"),
            row(AppCurrency.EUR, "en", "€100,000.50", "€100,001", "€100K", "-€1,130.00", "€1.5M", "123,456.7"),
            // Brazil writes its symbol before the amount, with a space
            row(AppCurrency.BRL, "pt-BR", "R\$_100.000,50", "R\$_100.001", "R\$_100K", "-R\$_1.130,00", "R\$_1,5M", "123.456,7"),
            row(AppCurrency.BRL, "en", "R$100,000.50", "R$100,001", "R$100K", "-R$1,130.00", "R$1.5M", "123,456.7"),
            // Yen: no decimals, rounded for display only
            row(AppCurrency.JPY, "en", "¥100,001", "¥100,001", "¥100K", "-¥1,130", "¥1.5M", "123,456.7"),
            row(AppCurrency.JPY, "de", "100.001_¥", "100.001_¥", "100K_¥", "-1.130_¥", "1,5M_¥", "123.456,7"),
            // A symbol made of letters is kept apart from the number
            row(AppCurrency.AED, "en", "AED_100,000.50", "AED_100,001", "AED_100K", "-AED_1,130.00", "AED_1.5M", "123,456.7"),
            row(AppCurrency.CHF, "de", "100.000,50_CHF", "100.001_CHF", "100K_CHF", "-1.130,00_CHF", "1,5M_CHF", "123.456,7"),
            row(AppCurrency.GBP, "en", "£100,000.50", "£100,001", "£100K", "-£1,130.00", "£1.5M", "123,456.7")
        )

        private fun row(vararg values: Any): Array<Any> = arrayOf(*values)
    }
}
