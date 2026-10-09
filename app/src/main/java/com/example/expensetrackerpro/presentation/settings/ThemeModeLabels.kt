package com.example.expensetrackerpro.presentation.settings

import androidx.annotation.StringRes
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.ThemeMode

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
