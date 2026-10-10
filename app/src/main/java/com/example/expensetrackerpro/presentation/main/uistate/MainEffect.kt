package com.example.expensetrackerpro.presentation.main.uistate

import androidx.annotation.StringRes
import com.example.expensetrackerpro.domain.models.Transaction

/** Things the app does once, as opposed to state it keeps showing. */
sealed interface MainEffect {
    /** [transaction] was just deleted, on whichever screen, and can still be restored. */
    data class ShowUndoDelete(val transaction: Transaction) : MainEffect
    data class ShowRestoreFailed(@StringRes val messageRes: Int) : MainEffect
}
