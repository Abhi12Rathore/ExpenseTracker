package com.example.expensetrackerpro.domain.currency

fun interface RegionCurrency {

    /** The currency of the region the phone is set to, or null when it has none or the app doesn't offer it. */
    fun currency(): AppCurrency?
}
