package com.example.expensetrackerpro.domain.usecase.app

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.usecase.category.LocalizeDefaultCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.currency.LoadCurrencyUseCase
import javax.inject.Inject

/** What has to be done before the first screen is drawn. Run on every start; running it again changes nothing. */
class PrepareAppUseCase @Inject constructor(
    private val loadCurrency: LoadCurrencyUseCase,
    private val localizeDefaultCategories: LocalizeDefaultCategoriesUseCase
) {

    /** Reads the currency amounts are shown in, then puts the built-in categories into the app's language. */
    suspend operator fun invoke(): Outcome<Unit> {
        loadCurrency()
        return localizeDefaultCategories()
    }
}
