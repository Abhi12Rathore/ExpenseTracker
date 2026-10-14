package com.example.expensetrackerpro.presentation.common.currency

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.utils.CurrencyFormatter
import java.util.Locale

/**
 * The app's [CurrencyFormatter], for Composables. MainActivity provides the injected one, which
 * follows the app's currency. The default is only ever used by @Preview, which has no activity:
 * it writes rupees.
 */
val LocalCurrencyFormatter = staticCompositionLocalOf {
    CurrencyFormatter(
        currencyProvider = { AppCurrency.INR },
        localeProvider = { Locale.ENGLISH }
    )
}
