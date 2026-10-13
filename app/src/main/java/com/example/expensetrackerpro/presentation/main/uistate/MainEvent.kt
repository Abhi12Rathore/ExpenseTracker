package com.example.expensetrackerpro.presentation.main.uistate

import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.Transaction

sealed interface MainEvent {
    // The activity was created: the app has started, or its language has changed
    data object ActivityCreated : MainEvent

    // The whole app left or returned to the foreground (not a rotation or a dialog)
    data object AppBackgrounded : MainEvent
    data object AppForegrounded : MainEvent
    data object UnlockClicked : MainEvent
    data object PromptLaunched : MainEvent
    data class AuthFinished(val result: AuthResult) : MainEvent

    /** Undo tapped on the snackbar shown for a deleted [transaction]. */
    data class UndoDelete(val transaction: Transaction) : MainEvent
}
