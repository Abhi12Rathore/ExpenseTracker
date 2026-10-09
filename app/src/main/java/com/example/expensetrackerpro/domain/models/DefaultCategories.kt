package com.example.expensetrackerpro.domain.models

/** The categories every new database starts with. */
object DefaultCategories {

    val expense: List<Category> = listOf(
        expense("Food & Dining", CategoryIcon.RESTAURANT),
        expense("Groceries", CategoryIcon.SHOPPING_BASKET),
        expense("Transport", CategoryIcon.DIRECTIONS_CAR),
        expense("Fuel", CategoryIcon.LOCAL_GAS_STATION),
        expense("Shopping", CategoryIcon.SHOPPING_BAG),
        expense("Bills & Utilities", CategoryIcon.RECEIPT_LONG),
        expense("Rent & Home", CategoryIcon.HOME),
        expense("Health", CategoryIcon.MEDICAL_SERVICES),
        expense("Education", CategoryIcon.SCHOOL),
        expense("Entertainment", CategoryIcon.MOVIE),
        expense("Travel", CategoryIcon.FLIGHT),
        expense("Subscriptions", CategoryIcon.SUBSCRIPTIONS),
        expense("Personal Care", CategoryIcon.SPA),
        expense("Gifts & Donations", CategoryIcon.CARD_GIFTCARD),
        expense("EMI & Loans", CategoryIcon.CREDIT_CARD),
        expense("Other", CategoryIcon.CATEGORY)
    )

    val income: List<Category> = listOf(
        income("Salary", CategoryIcon.PAYMENTS),
        income("Freelance", CategoryIcon.LAPTOP),
        income("Business", CategoryIcon.STOREFRONT),
        income("Investments", CategoryIcon.TRENDING_UP),
        income("Interest", CategoryIcon.SAVINGS),
        income("Rental Income", CategoryIcon.APARTMENT),
        income("Refunds & Cashback", CategoryIcon.CURRENCY_EXCHANGE),
        income("Gifts Received", CategoryIcon.REDEEM),
        income("Other Income", CategoryIcon.ACCOUNT_BALANCE_WALLET)
    )

    val all: List<Category> = expense + income

    private fun expense(name: String, icon: CategoryIcon) =
        Category(name = name, type = TransactionType.EXPENSE, icon = icon, isCustom = false)

    private fun income(name: String, icon: CategoryIcon) =
        Category(name = name, type = TransactionType.INCOME, icon = icon, isCustom = false)
}
