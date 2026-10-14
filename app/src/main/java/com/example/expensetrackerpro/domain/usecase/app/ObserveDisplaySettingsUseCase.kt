package com.example.expensetrackerpro.domain.usecase.app

import com.example.expensetrackerpro.domain.models.DisplaySettings
import com.example.expensetrackerpro.domain.usecase.currency.ObserveCurrencyUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveDisplaySettingsUseCase @Inject constructor(
    private val getThemeMode: GetThemeModeUseCase,
    private val observeCurrency: ObserveCurrencyUseCase
) {

    /** The theme and the currency, again whenever either changes. Starts once the theme has been read. */
    operator fun invoke(): Flow<DisplaySettings> =
        combine(getThemeMode(), observeCurrency(), ::DisplaySettings)
}
