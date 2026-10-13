package com.example.expensetrackerpro.data.language

import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.locale.DeviceLanguages
import com.example.expensetrackerpro.domain.repository.AppLanguageRepository
import com.example.expensetrackerpro.domain.usecase.language.ResolveAppLocaleUseCase
import java.util.Locale
import javax.inject.Inject

class AndroidAppLocaleProvider @Inject constructor(
    private val appLanguageRepository: AppLanguageRepository,
    private val deviceLanguages: DeviceLanguages,
    private val resolveAppLocale: ResolveAppLocaleUseCase
) : AppLocaleProvider {

    override fun current(): Locale = resolveAppLocale(appLanguageRepository.choice(), deviceLanguages.locales())
}
