package com.example.expensetrackerpro.data.currency

import com.example.expensetrackerpro.domain.currency.AppCurrency
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultCurrencyProviderTest {

    @Test
    fun amountsAreInRupees() {
        assertEquals(AppCurrency.INR, DefaultCurrencyProvider().current())
    }
}
