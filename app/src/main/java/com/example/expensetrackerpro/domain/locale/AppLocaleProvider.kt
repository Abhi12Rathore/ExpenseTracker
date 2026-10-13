package com.example.expensetrackerpro.domain.locale

import java.util.Locale

fun interface AppLocaleProvider {

    /** The locale the app is shown in now: the language chosen for it, or the one it takes from the phone. */
    fun current(): Locale
}
