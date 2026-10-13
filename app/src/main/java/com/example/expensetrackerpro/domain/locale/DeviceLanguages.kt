package com.example.expensetrackerpro.domain.locale

import java.util.Locale

interface DeviceLanguages {

    /** The languages set on the phone itself, most preferred first, whatever the app is shown in. */
    fun locales(): List<Locale>
}
