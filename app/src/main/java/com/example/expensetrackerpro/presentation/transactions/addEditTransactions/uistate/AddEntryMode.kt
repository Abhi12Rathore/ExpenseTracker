package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

/**
 * How a new transaction is started: the long press on the bottom bar's + and the launcher's app
 * shortcuts open the form ready for one of these. Carried by its [name] in the route.
 */
enum class AddEntryMode {
    /** The form as a tap on + opens it, the keypad up for the amount. */
    MANUAL,

    /** The quick-add bar focused, ready to type into. */
    QUICK_ADD,

    /** The bill scanner open. */
    SCAN,

    /** The speech recognizer listening. */
    VOICE;

    companion object {
        /** The mode named [name], or [MANUAL] for no name or one this version doesn't know. */
        fun fromName(name: String?): AddEntryMode = entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MANUAL
    }
}
