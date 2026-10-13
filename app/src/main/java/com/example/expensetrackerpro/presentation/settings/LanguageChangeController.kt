package com.example.expensetrackerpro.presentation.settings

import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.usecase.language.ForgetLanguageChangeUseCase
import com.example.expensetrackerpro.domain.usecase.language.GetLanguageOptionsUseCase
import com.example.expensetrackerpro.domain.usecase.language.ObserveLanguageChangeUseCase
import com.example.expensetrackerpro.domain.usecase.language.UndoLanguageChangeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** What Settings shows about the app's language. */
data class LanguageSettingState(
    val choice: LanguageChoice = LanguageChoice.SystemDefault,
    // A change that was just applied, while it can still be undone
    val change: LanguageChange? = null
)

/**
 * The Language row on Settings and the Undo offered after a change. One belongs to each
 * [SettingsViewModel], which shows its state and forwards its events.
 */
class LanguageChangeController @Inject constructor(
    private val getLanguageOptions: GetLanguageOptionsUseCase,
    observeLanguageChange: ObserveLanguageChangeUseCase,
    private val undoLanguageChange: UndoLanguageChangeUseCase,
    private val forgetLanguageChange: ForgetLanguageChangeUseCase
) {
    private val choice = MutableStateFlow(getLanguageOptions().current)

    val state: Flow<LanguageSettingState> =
        combine(choice, observeLanguageChange()) { choice, change -> LanguageSettingState(choice, change) }

    /** Reads the language again: it changes on another screen, or in the phone's settings for the app. */
    fun refresh() {
        choice.value = getLanguageOptions().current
    }

    fun undo() = undoLanguageChange()

    /** The Undo was offered and not taken. */
    fun onChangeShown() = forgetLanguageChange()
}
