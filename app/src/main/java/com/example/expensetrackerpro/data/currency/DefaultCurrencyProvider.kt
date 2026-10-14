package com.example.expensetrackerpro.data.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import javax.inject.Inject

/** Always rupees, until the currency can be chosen in Settings and is kept in DataStore. */
class DefaultCurrencyProvider @Inject constructor() : CurrencyProvider {

    override fun current(): AppCurrency = AppCurrency.INR
}
