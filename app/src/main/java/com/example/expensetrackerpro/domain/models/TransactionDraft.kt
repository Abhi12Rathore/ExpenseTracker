package com.example.expensetrackerpro.domain.models

/**
 * What could be worked out about a transaction from some source, such as a scanned bill or a
 * sentence typed into quick add. Every part is optional: a null was not found, and the form field
 * it belongs to is left alone.
 */
data class TransactionDraft(
    val amount: Money? = null,
    /** Other amounts that might be the total instead, largest first. Never includes [amount]. */
    val otherAmounts: List<Money> = emptyList(),
    val title: String? = null,
    /** The date as a date picker reports one: midnight UTC of the calendar day. */
    val dateMillis: Long? = null,
    val note: String? = null,
    val categoryHint: CategoryHint? = null,
    /** Whether it is money in or out, when the source says so. Null leaves the form's type. */
    val type: TransactionType? = null,
    /** A category already chosen, e.g. one the user picked for this title before. Wins over [categoryHint]. */
    val categoryId: CategoryId? = null
)

/**
 * The kind of spending a draft looks like. [categories] are the built-in categories it stands
 * for, most fitting first; it is only a suggestion, and none if the user has renamed or deleted
 * them all.
 */
enum class CategoryHint(val categories: List<BuiltInCategory>) {
    FOOD(listOf(BuiltInCategory.FOOD_DINING)),
    GROCERIES(listOf(BuiltInCategory.GROCERIES)),
    FUEL(listOf(BuiltInCategory.FUEL, BuiltInCategory.TRANSPORT)),
    TRANSPORT(listOf(BuiltInCategory.TRANSPORT)),
    HEALTH(listOf(BuiltInCategory.HEALTH)),
    SHOPPING(listOf(BuiltInCategory.SHOPPING)),
    BILLS(listOf(BuiltInCategory.BILLS_UTILITIES)),
    ENTERTAINMENT(listOf(BuiltInCategory.ENTERTAINMENT)),
    TRAVEL(listOf(BuiltInCategory.TRAVEL)),
    RENT(listOf(BuiltInCategory.RENT_HOME)),
    EDUCATION(listOf(BuiltInCategory.EDUCATION)),
    SUBSCRIPTIONS(listOf(BuiltInCategory.SUBSCRIPTIONS)),
    PERSONAL_CARE(listOf(BuiltInCategory.PERSONAL_CARE)),
    EMI(listOf(BuiltInCategory.EMI_LOANS)),

    SALARY(listOf(BuiltInCategory.SALARY)),
    FREELANCE(listOf(BuiltInCategory.FREELANCE)),
    INTEREST(listOf(BuiltInCategory.INTEREST)),
    REFUND(listOf(BuiltInCategory.REFUNDS_CASHBACK)),
    GIFT_RECEIVED(listOf(BuiltInCategory.GIFTS_RECEIVED))
}
