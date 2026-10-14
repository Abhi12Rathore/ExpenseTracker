package com.example.expensetrackerpro.data.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.fakes.FakeDeviceLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AndroidRegionCurrencyTest {

    private val languages = FakeDeviceLanguages()
    private val regionCurrency = AndroidRegionCurrency(languages)

    private fun currencyFor(vararg tags: String): AppCurrency? {
        languages.list = tags.map(Locale::forLanguageTag)
        return regionCurrency.currency()
    }

    @Test
    fun theFirstLanguagesCountry_decides() {
        assertEquals(AppCurrency.INR, currencyFor("en-IN"))
        assertEquals(AppCurrency.USD, currencyFor("en-US", "en-IN"))
        assertEquals(AppCurrency.EUR, currencyFor("de-DE"))
        assertEquals(AppCurrency.BRL, currencyFor("pt-BR"))
        assertEquals(AppCurrency.AED, currencyFor("ar-AE"))
        assertEquals(AppCurrency.JPY, currencyFor("ja-JP"))
    }

    @Test
    fun noCountry_orACurrencyTheAppDoesntOffer_isNone() {
        assertNull(currencyFor("en"))
        assertNull(currencyFor())
        // Kuwaiti dinar: three decimals, which Money can't hold
        assertNull(currencyFor("ar-KW"))
    }
}
