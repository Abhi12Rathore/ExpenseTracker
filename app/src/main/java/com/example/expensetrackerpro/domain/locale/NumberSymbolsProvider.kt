package com.example.expensetrackerpro.domain.locale

import com.example.expensetrackerpro.domain.currency.NumberSymbols
import java.util.Locale

fun interface NumberSymbolsProvider {

    /** The decimal and grouping separators [locale] writes numbers with. Digits are always Western. */
    fun symbolsFor(locale: Locale): NumberSymbols
}
