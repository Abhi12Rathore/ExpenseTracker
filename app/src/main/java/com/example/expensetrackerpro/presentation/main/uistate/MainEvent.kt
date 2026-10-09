package com.example.expensetrackerpro.presentation.main.uistate

import com.example.expensetrackerpro.domain.models.AuthResult

sealed interface MainEvent {
    // The whole app left or returned to the foreground (not a rotation or a dialog)
    data object AppBackgrounded : MainEvent
    data object AppForegrounded : MainEvent
    data object UnlockClicked : MainEvent
    data object PromptLaunched : MainEvent
    data class AuthFinished(val result: AuthResult) : MainEvent
}
