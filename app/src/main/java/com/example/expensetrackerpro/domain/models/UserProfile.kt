package com.example.expensetrackerpro.domain.models

/** Who the app greets. [name] is blank until the user has entered one. */
data class UserProfile(val name: String = "") {

    /** The letter the avatar shows, or null while there is no name to take it from. */
    val initial: Char?
        get() = name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()
}
