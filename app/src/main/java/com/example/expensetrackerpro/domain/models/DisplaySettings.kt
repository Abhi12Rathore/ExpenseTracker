package com.example.expensetrackerpro.domain.models

import com.example.expensetrackerpro.domain.currency.AppCurrency

/** The settings every screen is drawn with. */
data class DisplaySettings(
    val themeMode: ThemeMode,
    val currency: AppCurrency
)
