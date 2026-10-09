package com.example.expensetrackerpro.domain.models

/** The sections the icon picker is divided into, in the order they are shown. */
enum class CategoryIconGroup { FOOD_AND_SHOPPING, HOME_AND_BILLS, GETTING_AROUND, HEALTH_AND_LIFE, MONEY }

/**
 * The pictures a category can carry. [key] is what gets stored, so it must never change once
 * released; the entry names and their order are free to.
 */
enum class CategoryIcon(val key: String, val group: CategoryIconGroup) {

    // Food & shopping
    RESTAURANT("restaurant", CategoryIconGroup.FOOD_AND_SHOPPING),
    SHOPPING_BASKET("shopping_basket", CategoryIconGroup.FOOD_AND_SHOPPING),
    SHOPPING_BAG("shopping_bag", CategoryIconGroup.FOOD_AND_SHOPPING),
    STOREFRONT("storefront", CategoryIconGroup.FOOD_AND_SHOPPING),
    LOCAL_CAFE("local_cafe", CategoryIconGroup.FOOD_AND_SHOPPING),
    FASTFOOD("fastfood", CategoryIconGroup.FOOD_AND_SHOPPING),
    LOCAL_PIZZA("local_pizza", CategoryIconGroup.FOOD_AND_SHOPPING),
    LOCAL_BAR("local_bar", CategoryIconGroup.FOOD_AND_SHOPPING),
    CHECKROOM("checkroom", CategoryIconGroup.FOOD_AND_SHOPPING),
    DEVICES("devices", CategoryIconGroup.FOOD_AND_SHOPPING),

    // Home & bills
    RECEIPT_LONG("receipt_long", CategoryIconGroup.HOME_AND_BILLS),
    HOME("home", CategoryIconGroup.HOME_AND_BILLS),
    SUBSCRIPTIONS("subscriptions", CategoryIconGroup.HOME_AND_BILLS),
    APARTMENT("apartment", CategoryIconGroup.HOME_AND_BILLS),
    BOLT("bolt", CategoryIconGroup.HOME_AND_BILLS),
    WATER_DROP("water_drop", CategoryIconGroup.HOME_AND_BILLS),
    WIFI("wifi", CategoryIconGroup.HOME_AND_BILLS),
    PHONE_ANDROID("phone_android", CategoryIconGroup.HOME_AND_BILLS),
    LOCAL_LAUNDRY_SERVICE("local_laundry_service", CategoryIconGroup.HOME_AND_BILLS),
    BUILD("build", CategoryIconGroup.HOME_AND_BILLS),

    // Getting around
    DIRECTIONS_CAR("directions_car", CategoryIconGroup.GETTING_AROUND),
    LOCAL_GAS_STATION("local_gas_station", CategoryIconGroup.GETTING_AROUND),
    FLIGHT("flight", CategoryIconGroup.GETTING_AROUND),
    DIRECTIONS_BUS("directions_bus", CategoryIconGroup.GETTING_AROUND),
    TRAIN("train", CategoryIconGroup.GETTING_AROUND),
    TWO_WHEELER("two_wheeler", CategoryIconGroup.GETTING_AROUND),
    LOCAL_TAXI("local_taxi", CategoryIconGroup.GETTING_AROUND),
    DIRECTIONS_BIKE("directions_bike", CategoryIconGroup.GETTING_AROUND),
    HOTEL("hotel", CategoryIconGroup.GETTING_AROUND),

    // Health & life
    MEDICAL_SERVICES("medical_services", CategoryIconGroup.HEALTH_AND_LIFE),
    SCHOOL("school", CategoryIconGroup.HEALTH_AND_LIFE),
    MOVIE("movie", CategoryIconGroup.HEALTH_AND_LIFE),
    SPA("spa", CategoryIconGroup.HEALTH_AND_LIFE),
    CARD_GIFTCARD("card_giftcard", CategoryIconGroup.HEALTH_AND_LIFE),
    FITNESS_CENTER("fitness_center", CategoryIconGroup.HEALTH_AND_LIFE),
    PETS("pets", CategoryIconGroup.HEALTH_AND_LIFE),
    CHILD_CARE("child_care", CategoryIconGroup.HEALTH_AND_LIFE),
    MENU_BOOK("menu_book", CategoryIconGroup.HEALTH_AND_LIFE),
    SPORTS_ESPORTS("sports_esports", CategoryIconGroup.HEALTH_AND_LIFE),
    VOLUNTEER_ACTIVISM("volunteer_activism", CategoryIconGroup.HEALTH_AND_LIFE),
    CATEGORY("category", CategoryIconGroup.HEALTH_AND_LIFE),

    // Money
    PAYMENTS("payments", CategoryIconGroup.MONEY),
    LAPTOP("laptop", CategoryIconGroup.MONEY),
    TRENDING_UP("trending_up", CategoryIconGroup.MONEY),
    SAVINGS("savings", CategoryIconGroup.MONEY),
    CURRENCY_EXCHANGE("currency_exchange", CategoryIconGroup.MONEY),
    REDEEM("redeem", CategoryIconGroup.MONEY),
    ACCOUNT_BALANCE_WALLET("account_balance_wallet", CategoryIconGroup.MONEY),
    CREDIT_CARD("credit_card", CategoryIconGroup.MONEY),
    ACCOUNT_BALANCE("account_balance", CategoryIconGroup.MONEY),
    WORK("work", CategoryIconGroup.MONEY),
    SHIELD("shield", CategoryIconGroup.MONEY),
    PERCENT("percent", CategoryIconGroup.MONEY),
    CURRENCY_RUPEE("currency_rupee", CategoryIconGroup.MONEY),
    RECEIPT("receipt", CategoryIconGroup.MONEY);

    companion object {

        /** What a category shows when nothing more specific was chosen. */
        val DEFAULT = CATEGORY

        private val byKey = entries.associateBy { it.key }

        /** The icon stored as [key], or [DEFAULT] for a key this version does not know. */
        fun fromKey(key: String): CategoryIcon = byKey[key] ?: DEFAULT

        fun inGroup(group: CategoryIconGroup): List<CategoryIcon> = entries.filter { it.group == group }
    }
}
