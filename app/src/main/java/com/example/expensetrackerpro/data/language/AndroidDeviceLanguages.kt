package com.example.expensetrackerpro.data.language

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import com.example.expensetrackerpro.domain.locale.DeviceLanguages
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class AndroidDeviceLanguages @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceLanguages {

    // The system's list, which the language chosen for the app does not change
    override fun locales(): List<Locale> {
        val locales = LocaleManagerCompat.getSystemLocales(context)

        return List(locales.size()) { locales[it] }.filterNotNull()
    }
}
