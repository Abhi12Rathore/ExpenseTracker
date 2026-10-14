package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.CurrencyProvider

/** Amounts shown in [currency] until a test changes it; counts how often it was asked. */
class FakeCurrencyProvider(var currency: AppCurrency = AppCurrency.INR) : CurrencyProvider {

    var reads = 0
        private set

    override fun current(): AppCurrency {
        reads++
        return currency
    }
}
