package com.example.expensetrackerpro.presentation.language.uistate

import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.models.LanguagePreview

data class LanguageUiState(
    // What the app is shown in now
    val current: LanguageChoice = LanguageChoice.SystemDefault,
    // The row that is ticked; nothing changes until it is applied
    val selected: LanguageChoice = LanguageChoice.SystemDefault,
    // The language the app is shown in while it follows the phone, and the phone's name for its own
    val systemLanguage: AppLanguage = AppLanguage.ENGLISH,
    val systemLanguageName: String = "",
    // The phone's other languages the app is translated into, then every language left
    val suggested: List<AppLanguage> = emptyList(),
    val others: List<AppLanguage> = emptyList(),
    // Home in the selected language; null while the selection is what the app is shown in
    val preview: LanguagePreview? = null,
    // True once the selection was confirmed, which closes the screen
    val isApplied: Boolean = false,
    // For the subject of a translation report
    val versionName: String = ""
) {
    /** The apply button is offered only for a language other than the current one. */
    val isApplyVisible: Boolean get() = selected != current

    /** The language the selected row stands for. */
    val selectedLanguage: AppLanguage
        get() = when (selected) {
            LanguageChoice.SystemDefault -> systemLanguage
            is LanguageChoice.Specific -> selected.language
        }
}
