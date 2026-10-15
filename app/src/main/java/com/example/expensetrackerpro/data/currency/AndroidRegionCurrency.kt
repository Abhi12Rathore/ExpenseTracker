package com.example.expensetrackerpro.data.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.RegionCurrency
import com.example.expensetrackerpro.domain.locale.DeviceLanguages
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/** The currency of the country of the phone's first language, e.g. USD for en-US. */
class AndroidRegionCurrency @Inject constructor(
    private val deviceLanguages: DeviceLanguages
) : RegionCurrency {

    override fun currency(): AppCurrency? {
        val country = deviceLanguages.locales().firstOrNull()?.country
        if (country.isNullOrEmpty()) return null

        // Throws for a code that isn't a country, and is null for a region without a currency (Antarctica)
        val currency = try {
            Currency.getInstance(Locale("", country))
        } catch (_: IllegalArgumentException) {
            null
        }

        return currency?.currencyCode?.let(AppCurrency::findByCode)
    }
}
