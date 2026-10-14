package com.example.expensetrackerpro.domain.usecase.transaction

import java.util.Locale
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.fakes.HINDI
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeCurrencyProvider
import com.example.expensetrackerpro.fakes.sanitizeAmountInput
import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizeAmountInputUseCaseTest {

    private val sanitize = sanitizeAmountInput()

    @Test
    fun digitsAndOneDecimalPoint_areKept() {
        assertEquals("1", sanitize("", "1"))
        assertEquals("12.", sanitize("12", "12."))
        assertEquals("12.5", sanitize("12.", "12.5"))
        assertEquals("12.50", sanitize("12.5", "12.50"))
    }

    @Test
    fun clearingTheField_isAllowed() {
        assertEquals("", sanitize("12", ""))
    }

    @Test
    fun thirdDecimal_isIgnored() {
        assertEquals("12.50", sanitize("12.50", "12.505"))
    }

    @Test
    fun leadingZeros_areDropped() {
        assertEquals("7", sanitize("0", "07"))
        assertEquals("0", sanitize("", "0"))
        assertEquals("0", sanitize("0", "00"))
        assertEquals("0.5", sanitize("0.", "0.5"))
    }

    @Test
    fun barePoint_getsAZeroInFront() {
        assertEquals("0.", sanitize("", "."))
        assertEquals("0.5", sanitize("", ".5"))
    }

    @Test
    fun aboveOneCrore_isIgnored() {
        assertEquals("10000000", sanitize("1000000", "10000000"))
        assertEquals("10000000", sanitize("10000000", "100000000"))
        assertEquals("10000000", sanitize("10000000", "10000000.1"))
    }

    @Test
    fun pastedJunk_leavesTheFieldAsItWas() {
        listOf("1e5", "NaN", "Infinity", "-5", "1,000", "₹500", "12 ", "abc", "1.2.3").forEach { pasted ->
            assertEquals(pasted, "42", sanitize("42", pasted))
        }
    }

    // --- The decimal key follows the language, decimals follow the currency ---

    private val currencies = FakeCurrencyProvider()
    private val locales = FakeAppLocaleProvider()
    private val sanitizeLocalized = sanitizeAmountInput(currencies, locales)

    @Test
    fun inALanguageThatWritesACommaDecimal_theCommaIsTheDecimalPoint() {
        locales.locale = Locale.GERMAN

        assertEquals("12.", sanitizeLocalized("12", "12,"))
        assertEquals("12.5", sanitizeLocalized("12.", "12.5"))
        assertEquals("0.", sanitizeLocalized("", ","))
    }

    @Test
    fun inALanguageThatWritesACommaDecimal_aDotIsTakenAsTheDecimalPointToo() {
        // Many keyboards' number pads have only a dot
        for (locale in listOf(Locale.GERMAN, Locale.FRENCH, Locale.forLanguageTag("es"), Locale.forLanguageTag("pt-BR"))) {
            locales.locale = locale

            assertEquals("$locale", "12.", sanitizeLocalized("12", "12."))
        }
    }

    @Test
    fun inALanguageThatWritesADotDecimal_aCommaIsRefused() {
        for (locale in listOf(Locale.ENGLISH, HINDI)) {
            locales.locale = locale

            assertEquals("$locale", "12", sanitizeLocalized("12", "12,"))
        }
    }

    @Test
    fun aSecondDecimalPoint_isRefused_whicheverKeyTypedIt() {
        locales.locale = Locale.GERMAN

        assertEquals("1.5", sanitizeLocalized("1.5", "1.5,"))
        assertEquals("1.5", sanitizeLocalized("1.5", "1,5."))
    }

    @Test
    fun yen_takeNoDecimalPoint() {
        currencies.currency = AppCurrency.JPY

        assertEquals("450", sanitizeLocalized("450", "450."))
        assertEquals("", sanitizeLocalized("", "."))
        assertEquals("4500", sanitizeLocalized("450", "4500"))

        locales.locale = Locale.GERMAN
        assertEquals("450", sanitizeLocalized("450", "450,"))
    }

    @Test
    fun yen_anAmountThatAlreadyHasDecimals_keepsThem() {
        // Saved in rupees, and opened again after switching to yen
        currencies.currency = AppCurrency.JPY

        assertEquals("450.5", sanitizeLocalized("450.50", "450.5"))
        assertEquals("450.", sanitizeLocalized("450.5", "450."))
        assertEquals("450", sanitizeLocalized("450.", "450"))
        // Once the point is gone it can't come back
        assertEquals("450", sanitizeLocalized("450", "450."))
    }

    @Test
    fun theLimit_isTheSameInEveryCurrencyAndLanguage() {
        currencies.currency = AppCurrency.EUR
        locales.locale = Locale.GERMAN

        assertEquals("10000000", sanitizeLocalized("10000000", "10000000,1"))
    }
}

