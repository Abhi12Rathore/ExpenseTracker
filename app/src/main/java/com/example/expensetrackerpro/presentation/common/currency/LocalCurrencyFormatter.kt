package com.example.expensetrackerpro.presentation.common.currency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.NumberSymbols
import com.example.expensetrackerpro.utils.CurrencyFormatter
import java.util.Locale

/**
 * The app's [CurrencyFormatter], for Composables. MainActivity provides the injected one, which
 * follows the app's currency. The default is only ever used by @Preview, which has no activity:
 * it writes rupees in English.
 */
val LocalCurrencyFormatter = staticCompositionLocalOf {
    CurrencyFormatter(
        currencyProvider = { AppCurrency.INR },
        localeProvider = { Locale.ENGLISH },
        numberSymbolsProvider = { NumberSymbols.ENGLISH }
    )
}

/**
 * The currency amounts are shown in. MainActivity provides it next to [LocalCurrencyFormatter]:
 * a change recomposes every screen, and a Composable that keeps formatted text in `remember`
 * keys it on this, so the text is written again in the new currency.
 */
val LocalAppCurrency = staticCompositionLocalOf { AppCurrency.INR }

/**
 * What decides how an amount is written: the currency and the app's language. Key a `remember`
 * that holds formatted text on this, so it is written again when either changes.
 */
@Composable
@ReadOnlyComposable
fun amountFormatKey(): Any = LocalAppCurrency.current to LocalConfiguration.current.locales[0]
