package com.example.expensetrackerpro.domain.models

/**
 * The categories the app comes with. [key] is what gets stored, so it must never change once
 * released; the entry names and their order are free to. A built-in has no name of its own:
 * it is shown under the name the app's language gives it.
 */
enum class BuiltInCategory(val key: String, val type: TransactionType, val icon: CategoryIcon) {

    FOOD_DINING("food_dining", TransactionType.EXPENSE, CategoryIcon.RESTAURANT),
    GROCERIES("groceries", TransactionType.EXPENSE, CategoryIcon.SHOPPING_BASKET),
    TRANSPORT("transport", TransactionType.EXPENSE, CategoryIcon.DIRECTIONS_CAR),
    FUEL("fuel", TransactionType.EXPENSE, CategoryIcon.LOCAL_GAS_STATION),
    SHOPPING("shopping", TransactionType.EXPENSE, CategoryIcon.SHOPPING_BAG),
    BILLS_UTILITIES("bills_utilities", TransactionType.EXPENSE, CategoryIcon.RECEIPT_LONG),
    RENT_HOME("rent_home", TransactionType.EXPENSE, CategoryIcon.HOME),
    HEALTH("health", TransactionType.EXPENSE, CategoryIcon.MEDICAL_SERVICES),
    EDUCATION("education", TransactionType.EXPENSE, CategoryIcon.SCHOOL),
    ENTERTAINMENT("entertainment", TransactionType.EXPENSE, CategoryIcon.MOVIE),
    TRAVEL("travel", TransactionType.EXPENSE, CategoryIcon.FLIGHT),
    SUBSCRIPTIONS("subscriptions", TransactionType.EXPENSE, CategoryIcon.SUBSCRIPTIONS),
    PERSONAL_CARE("personal_care", TransactionType.EXPENSE, CategoryIcon.SPA),
    GIFTS_DONATIONS("gifts_donations", TransactionType.EXPENSE, CategoryIcon.CARD_GIFTCARD),
    EMI_LOANS("emi_loans", TransactionType.EXPENSE, CategoryIcon.CREDIT_CARD),
    OTHER("other", TransactionType.EXPENSE, CategoryIcon.CATEGORY),

    SALARY("salary", TransactionType.INCOME, CategoryIcon.PAYMENTS),
    FREELANCE("freelance", TransactionType.INCOME, CategoryIcon.LAPTOP),
    BUSINESS("business", TransactionType.INCOME, CategoryIcon.STOREFRONT),
    INVESTMENTS("investments", TransactionType.INCOME, CategoryIcon.TRENDING_UP),
    INTEREST("interest", TransactionType.INCOME, CategoryIcon.SAVINGS),
    RENTAL_INCOME("rental_income", TransactionType.INCOME, CategoryIcon.APARTMENT),
    REFUNDS_CASHBACK("refunds_cashback", TransactionType.INCOME, CategoryIcon.CURRENCY_EXCHANGE),
    GIFTS_RECEIVED("gifts_received", TransactionType.INCOME, CategoryIcon.REDEEM),
    OTHER_INCOME("other_income", TransactionType.INCOME, CategoryIcon.ACCOUNT_BALANCE_WALLET);

    companion object {
        private val byKey = entries.associateBy { it.key }

        /** The built-in stored as [key], or null for a key this version does not know. */
        fun findByKey(key: String): BuiltInCategory? = byKey[key]
    }
}
