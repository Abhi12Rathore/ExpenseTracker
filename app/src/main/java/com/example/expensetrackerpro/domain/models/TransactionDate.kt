package com.example.expensetrackerpro.domain.models

/** How the date of a transaction form was chosen. */
sealed interface DateSelection {
    data object Today : DateSelection
    data object Yesterday : DateSelection

    /** A date picker result, which reports the chosen date as midnight UTC. */
    data class Picked(val utcDayMillis: Long) : DateSelection

    /** The date an existing transaction already has. */
    data class Stored(val millis: Long) : DateSelection
}

enum class DateChoice {
    TODAY, YESTERDAY, OTHER
}

/**
 * A resolved transaction date: the instant to store and which shortcut it corresponds to. For
 * a date picker, which works in midnight-UTC days, [pickerUtcMillis] is the date itself and
 * [maxPickerUtcMillis] is today, the latest day it may offer.
 */
data class TransactionDate(
    val millis: Long,
    val choice: DateChoice,
    val pickerUtcMillis: Long,
    val maxPickerUtcMillis: Long
)
