package com.example.expensetrackerpro.data.currency

import android.content.Context
import android.content.res.Configuration
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.CurrencyNames
import com.example.expensetrackerpro.utils.nameRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** The currencies' names, from the string resources of whichever language is asked for. */
@Singleton
class ResourceCurrencyNames @Inject constructor(
    @ApplicationContext private val context: Context
) : CurrencyNames {

    // A context per language asked for; there are only ever a few
    private val localizedContexts = ConcurrentHashMap<Locale, Context>()

    override fun nameOf(currency: AppCurrency, locale: Locale): String =
        localizedContexts.getOrPut(locale) { context.localizedFor(locale) }.getString(currency.nameRes())

    // Reads the resources of [locale] whatever language the app is shown in
    private fun Context.localizedFor(locale: Locale): Context =
        createConfigurationContext(Configuration(resources.configuration).apply { setLocale(locale) })
}
