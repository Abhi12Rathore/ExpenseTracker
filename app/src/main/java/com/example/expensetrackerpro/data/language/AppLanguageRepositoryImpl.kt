package com.example.expensetrackerpro.data.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.repository.AppLanguageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place the app's language is read from and handed to Android. AppCompat stores the
 * choice: in the system from Android 13, where it also shows in the phone's settings for the
 * app, and in its own file before that (autoStoreLocales in the manifest).
 */
@Singleton
class AppLanguageRepositoryImpl @Inject constructor() : AppLanguageRepository {

    private val _pendingChange = MutableStateFlow<LanguageChange?>(null)
    override val pendingChange: StateFlow<LanguageChange?> = _pendingChange.asStateFlow()

    override fun choice(): LanguageChoice {
        // Empty while the app follows the phone
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()

        return tag?.let(AppLanguage::findByTag)?.let(LanguageChoice::Specific) ?: LanguageChoice.SystemDefault
    }

    override fun setChoice(choice: LanguageChoice) {
        val locales = when (choice) {
            LanguageChoice.SystemDefault -> LocaleListCompat.getEmptyLocaleList()
            is LanguageChoice.Specific -> LocaleListCompat.forLanguageTags(choice.language.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    override fun setPendingChange(change: LanguageChange?) {
        _pendingChange.value = change
    }
}
