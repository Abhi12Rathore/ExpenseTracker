package com.example.expensetrackerpro.data.currency

import com.example.expensetrackerpro.domain.currency.NumberSymbols
import com.example.expensetrackerpro.domain.models.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Only the separators are taken, and every language the app has writes them as expected. */
class AndroidNumberSymbolsProviderTest {

    private val provider = AndroidNumberSymbolsProvider()

    private fun symbols(tag: String): NumberSymbols = provider.symbolsFor(Locale.forLanguageTag(tag))

    @Test
    fun englishAndHindi_writeADotDecimal() {
        assertEquals(NumberSymbols.ENGLISH, symbols("en"))
        assertEquals(NumberSymbols.ENGLISH, symbols("hi"))
    }

    @Test
    fun theOtherLanguages_writeACommaDecimal() {
        for (tag in listOf("es", "fr", "de", "pt-BR")) {
            assertEquals(tag, ',', symbols(tag).decimalSeparator)
        }
        assertEquals('.', symbols("de").groupingSeparator)
        assertEquals('.', symbols("pt-BR").groupingSeparator)
        // French groups with a space of some width, never a dot
        assertTrue(symbols("fr").groupingSeparator.isWhitespace() || symbols("fr").groupingSeparator == ' ')
    }

    @Test
    fun noLanguageTheAppHas_usesTheSameCharacterForBoth() {
        AppLanguage.entries.forEach { language ->
            val symbols = symbols(language.tag)
            assertTrue(language.tag, symbols.decimalSeparator != symbols.groupingSeparator)
        }
    }
}
