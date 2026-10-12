package com.example.expensetrackerpro.domain.models

/**
 * What could be worked out about a transaction from some source, such as a scanned bill. Every
 * part is optional: a null was not found, and the form field it belongs to is left alone.
 */
data class TransactionDraft(
    val amount: Money? = null,
    /** Other amounts that might be the total instead, largest first. Never includes [amount]. */
    val otherAmounts: List<Money> = emptyList(),
    val title: String? = null,
    /** The date as a date picker reports one: midnight UTC of the calendar day. */
    val dateMillis: Long? = null,
    val note: String? = null,
    val categoryHint: CategoryHint? = null
)

/**
 * The kind of spending a draft looks like. [categoryNames] are the default categories it stands
 * for, most fitting first; it is only a suggestion, and none if the user has deleted them all.
 */
enum class CategoryHint(val categoryNames: List<String>) {
    FOOD(listOf("Food & Dining")),
    GROCERIES(listOf("Groceries")),
    FUEL(listOf("Fuel", "Transport")),
    TRANSPORT(listOf("Transport")),
    HEALTH(listOf("Health")),
    SHOPPING(listOf("Shopping")),
    BILLS(listOf("Bills & Utilities")),
    ENTERTAINMENT(listOf("Entertainment")),
    TRAVEL(listOf("Travel"))
}
