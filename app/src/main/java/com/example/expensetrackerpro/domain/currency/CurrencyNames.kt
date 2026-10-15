package com.example.expensetrackerpro.domain.currency

import java.util.Locale

fun interface CurrencyNames {

    /** [currency]'s name as [locale] writes it, e.g. `US Dollar` or `US-Dollar`. */
    fun nameOf(currency: AppCurrency, locale: Locale): String
}
