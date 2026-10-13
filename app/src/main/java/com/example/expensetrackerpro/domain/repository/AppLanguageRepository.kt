package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import kotlinx.coroutines.flow.StateFlow

/** The language the app is shown in, as the user chose it. */
interface AppLanguageRepository {

    /** Read each time: the choice can also be changed in the phone's own settings for the app. */
    fun choice(): LanguageChoice

    /** Takes effect at once: every screen is drawn again in the new language. */
    fun setChoice(choice: LanguageChoice)

    /** The change that was last applied, for as long as it can be undone. Not kept across a restart. */
    val pendingChange: StateFlow<LanguageChange?>

    fun setPendingChange(change: LanguageChange?)
}
