package com.example.expensetrackerpro.utils

import androidx.annotation.StringRes
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.currency.AppCurrency

/** The string that names [this] currency, e.g. `US Dollar`. */
@StringRes
fun AppCurrency.nameRes(): Int = when (this) {
    AppCurrency.INR -> R.string.currency_name_inr
    AppCurrency.USD -> R.string.currency_name_usd
    AppCurrency.EUR -> R.string.currency_name_eur
    AppCurrency.GBP -> R.string.currency_name_gbp
    AppCurrency.AED -> R.string.currency_name_aed
    AppCurrency.SAR -> R.string.currency_name_sar
    AppCurrency.QAR -> R.string.currency_name_qar
    AppCurrency.SGD -> R.string.currency_name_sgd
    AppCurrency.AUD -> R.string.currency_name_aud
    AppCurrency.CAD -> R.string.currency_name_cad
    AppCurrency.NZD -> R.string.currency_name_nzd
    AppCurrency.JPY -> R.string.currency_name_jpy
    AppCurrency.CNY -> R.string.currency_name_cny
    AppCurrency.HKD -> R.string.currency_name_hkd
    AppCurrency.MYR -> R.string.currency_name_myr
    AppCurrency.THB -> R.string.currency_name_thb
    AppCurrency.IDR -> R.string.currency_name_idr
    AppCurrency.NPR -> R.string.currency_name_npr
    AppCurrency.LKR -> R.string.currency_name_lkr
    AppCurrency.BDT -> R.string.currency_name_bdt
    AppCurrency.PKR -> R.string.currency_name_pkr
    AppCurrency.ZAR -> R.string.currency_name_zar
    AppCurrency.BRL -> R.string.currency_name_brl
    AppCurrency.CHF -> R.string.currency_name_chf
}
