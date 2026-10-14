package com.example.expensetrackerpro.domain.currency

fun interface CurrencyProvider {

    /** The currency amounts are shown in now. */
    fun current(): AppCurrency
}
