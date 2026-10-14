package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.CurrencyNames
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/**
 * Which currencies a search matches: by code (`usd`), by name in the app's language
 * (`dólar`, `dolar`) or by English name, ignoring case and accents. A blank query matches all.
 */
class SearchCurrenciesUseCase @Inject constructor(
    private val names: CurrencyNames,
    private val localeProvider: AppLocaleProvider
) {

    operator fun invoke(query: String): Set<AppCurrency> {
        val search = query.trim().folded()
        if (search.isEmpty()) return AppCurrency.entries.toSet()

        val locale = localeProvider.current()
        return AppCurrency.entries.filterTo(mutableSetOf()) { currency ->
            listOf(currency.code, names.nameOf(currency, locale), names.nameOf(currency, Locale.ENGLISH))
                .any { it.folded().contains(search) }
        }
    }

    // Lower case without accents: "Dólar" and "dolar" match
    private fun String.folded(): String =
        Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(COMBINING_MARKS, "")

    private companion object {
        val COMBINING_MARKS = Regex("\\p{Mn}+")
    }
}
