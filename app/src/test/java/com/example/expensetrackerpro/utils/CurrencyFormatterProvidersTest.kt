package com.example.expensetrackerpro.utils

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.DigitGrouping
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeCurrencyProvider
import com.example.expensetrackerpro.fakes.HINDI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale

/** How [CurrencyFormatter] uses what it is given: the currency and the app's language. */
class CurrencyFormatterProvidersTest {

    private val currencies = FakeCurrencyProvider()
    private val locales = FakeAppLocaleProvider()
    private val formatter = CurrencyFormatter(currencies, locales)

    @Test
    fun rupees_areTheOnlyCurrency_writtenWithTwoDecimalsAndIndianGrouping() {
        assertEquals(listOf(AppCurrency.INR), AppCurrency.entries)
        assertEquals("INR", AppCurrency.INR.code)
        assertEquals("₹", AppCurrency.INR.symbol)
        assertEquals(2, AppCurrency.INR.fractionDigits)
        assertEquals(DigitGrouping.INDIAN, AppCurrency.INR.grouping)
    }

    @Test
    fun theCurrencyIsAskedForOnEveryCall_notOnceWhenCreated() {
        assertEquals(0, currencies.reads)

        formatter.format(Money.ofRupees(1))
        formatter.formatWhole(Money.ofRupees(1))
        formatter.formatShort(Money.ofRupees(1))
        formatter.groupInput("1")

        assertEquals(4, currencies.reads)
    }

    @Test
    fun symbol_isTheCurrencys() {
        assertEquals(AppCurrency.INR.symbol, formatter.symbol)
        assertEquals("${formatter.symbol}5.00", formatter.format(Money.ofRupees(5)))
    }

    @Test
    fun groupInput_groupsTheWholePart_andLeavesTheDecimalsAsTyped() {
        assertEquals("", formatter.groupInput(""))
        assertEquals("999", formatter.groupInput("999"))
        assertEquals("1,23,456.7", formatter.groupInput("123456.7"))
        assertEquals("12,34,567.", formatter.groupInput("1234567."))
        assertEquals(".5", formatter.groupInput(".5"))
    }

    @Test
    fun groupInput_separatesWithGroupingSeparator_only() {
        val grouped = formatter.groupInput("1234567")

        assertEquals("1234567", grouped.filter { it != formatter.groupingSeparator })
        assertEquals(2, grouped.count { it == formatter.groupingSeparator })
    }

    @Test
    fun theAppsLanguage_doesNotChangeHowRupeesAreWritten() {
        val amount = Money(-12_34_567_89)
        val inEnglish = listOf(formatter.format(amount), formatter.formatWhole(amount), formatter.formatShort(amount))

        for (locale in listOf(HINDI, Locale.GERMAN, Locale.FRENCH, Locale.forLanguageTag("pt-BR"))) {
            locales.locale = locale
            val written = listOf(formatter.format(amount), formatter.formatWhole(amount), formatter.formatShort(amount))

            assertEquals("in $locale", inEnglish, written)
        }
    }

    @Test
    fun digitsStayWestern_inEveryLanguage() {
        locales.locale = HINDI

        assertFalse(formatter.format(Money.ofRupees(1234567)).any { it.isDigit() && it !in '0'..'9' })
    }
}
