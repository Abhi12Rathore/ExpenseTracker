package com.example.expensetrackerpro.utils

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.DigitGrouping
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeCurrencyProvider
import com.example.expensetrackerpro.fakes.FakeNumberSymbols
import com.example.expensetrackerpro.fakes.HINDI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** How [CurrencyFormatter] uses what it is given: the currency and the app's language. */
class CurrencyFormatterProvidersTest {

    private val currencies = FakeCurrencyProvider()
    private val locales = FakeAppLocaleProvider()
    private val formatter = CurrencyFormatter(currencies, locales, FakeNumberSymbols())

    @Test
    fun onlyRupees_useIndianGrouping_andOnlyYen_haveNoDecimals() {
        assertEquals(listOf(AppCurrency.INR), AppCurrency.entries.filter { it.grouping == DigitGrouping.INDIAN })
        assertEquals(listOf(AppCurrency.JPY), AppCurrency.entries.filter { it.fractionDigits == 0 })
        assertTrue(AppCurrency.entries.all { it.fractionDigits in 0..2 })
    }

    @Test
    fun everyCurrency_hasItsOwnCode_andIsFoundByIt() {
        assertEquals(AppCurrency.entries.size, AppCurrency.entries.map { it.code }.toSet().size)
        AppCurrency.entries.forEach { assertEquals(it, AppCurrency.findByCode(it.code)) }
        assertEquals(AppCurrency.USD, AppCurrency.findByCode("usd"))
        assertEquals(null, AppCurrency.findByCode("KWD"))
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
    fun aCurrencyPassedIn_isWrittenInstead_withoutAskingForTheCurrentOne() {
        assertEquals("$100,000", formatter.formatWhole(Money.ofRupees(100_000), AppCurrency.USD))
        assertEquals("₹1,00,000", formatter.formatWhole(Money.ofRupees(100_000), AppCurrency.INR))
        assertEquals(0, currencies.reads)
    }

    @Test
    fun aChangeOfCurrency_isWrittenAtTheNextCall() {
        val amount = Money.ofRupees(100_000)
        assertEquals("₹1,00,000.00", formatter.format(amount))

        currencies.currency = AppCurrency.USD

        assertEquals("$100,000.00", formatter.format(amount))
    }

    @Test
    fun symbol_andSeparators_areTheCurrentOnes() {
        assertEquals(AppCurrency.INR.symbol, formatter.symbol)
        assertEquals(',', formatter.groupingSeparator)
        assertEquals('.', formatter.decimalSeparator)

        currencies.currency = AppCurrency.EUR
        locales.locale = Locale.GERMAN

        assertEquals("€", formatter.symbol)
        assertEquals('.', formatter.groupingSeparator)
        assertEquals(',', formatter.decimalSeparator)
    }

    @Test
    fun theSymbolGoesAfter_onlyInLanguagesThatWriteItThere() {
        for ((tag, after) in listOf("en" to false, "hi" to false, "pt-BR" to false, "es" to true, "fr" to true, "de" to true)) {
            locales.locale = Locale.forLanguageTag(tag)

            assertEquals(tag, after, formatter.isSymbolAfter)
        }
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
    fun rupees_areWrittenTheSameInEnglishAndHindi() {
        val amount = Money(-12_34_567_89)
        val inEnglish = listOf(formatter.format(amount), formatter.formatWhole(amount), formatter.formatShort(amount))

        locales.locale = HINDI

        assertEquals(inEnglish, listOf(formatter.format(amount), formatter.formatWhole(amount), formatter.formatShort(amount)))
    }

    @Test
    fun digitsStayWestern_inEveryLanguage() {
        for (locale in listOf(HINDI, Locale.GERMAN, Locale.FRENCH, Locale.forLanguageTag("pt-BR"))) {
            locales.locale = locale

            assertFalse(formatter.format(Money.ofRupees(1234567)).any { it.isDigit() && it !in '0'..'9' })
        }
    }

    @Test
    fun aLanguageTheAppDoesntHave_writesAsEnglishDoes() {
        currencies.currency = AppCurrency.EUR
        locales.locale = Locale.JAPANESE

        assertEquals("€1,234.50", formatter.format(Money(1_234_50)))
    }
}
