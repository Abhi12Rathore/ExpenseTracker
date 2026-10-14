package com.example.expensetrackerpro.data.currency

import com.example.expensetrackerpro.domain.currency.NumberSymbols
import com.example.expensetrackerpro.domain.locale.NumberSymbolsProvider
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** The separators Android writes numbers with in a language. Only the separators: digits stay Western. */
@Singleton
class AndroidNumberSymbolsProvider @Inject constructor() : NumberSymbolsProvider {

    // Asked for on every amount drawn, and the app is only ever in a few languages
    private val cache = ConcurrentHashMap<Locale, NumberSymbols>()

    override fun symbolsFor(locale: Locale): NumberSymbols = cache.getOrPut(locale) {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        NumberSymbols(
            decimalSeparator = symbols.decimalSeparator,
            groupingSeparator = symbols.groupingSeparator
        )
    }
}
