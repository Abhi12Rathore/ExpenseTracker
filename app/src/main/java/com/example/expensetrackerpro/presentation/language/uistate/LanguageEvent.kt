package com.example.expensetrackerpro.presentation.language.uistate

import com.example.expensetrackerpro.domain.models.LanguageChoice

sealed interface LanguageEvent {
    data class LanguageSelected(val choice: LanguageChoice) : LanguageEvent

    // The button under the list: confirms the selected language
    data object ApplyClicked : LanguageEvent

    // The screen has closed after a confirmation, so the app can now be drawn in the new language
    data object ClosedAfterApply : LanguageEvent
}
