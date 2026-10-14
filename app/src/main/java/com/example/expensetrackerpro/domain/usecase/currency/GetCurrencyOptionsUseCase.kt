package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.CurrencyNames
import com.example.expensetrackerpro.domain.currency.RegionCurrency
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.models.CurrencyOptions
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import java.text.Collator
import javax.inject.Inject

/** What the Currency screen offers: the current one and the phone region's first, then the rest by name. */
class GetCurrencyOptionsUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val regionCurrency: RegionCurrency,
    private val names: CurrencyNames,
    private val localeProvider: AppLocaleProvider
) {

    operator fun invoke(): CurrencyOptions {
        val current = currencyRepository.currency.value
        val suggested = listOfNotNull(current, regionCurrency.currency()).distinct()
        val locale = localeProvider.current()
        // Sorted as the language sorts its words: "É" next to "E", not after "Z"
        val collator = Collator.getInstance(locale)

        return CurrencyOptions(
            current = current,
            suggested = suggested,
            others = (AppCurrency.entries - suggested.toSet())
                .sortedWith(compareBy(collator) { names.nameOf(it, locale) })
        )
    }
}
