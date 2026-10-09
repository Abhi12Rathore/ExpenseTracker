package com.example.expensetrackerpro.presentation.common.category

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryIconGroup
import com.example.expensetrackerpro.ui.theme.CategoryPalette
import com.example.expensetrackerpro.ui.theme.extendedColors

/** How a [CategoryIcon] is drawn: its vector, the palette hue that tints it and its spoken name. */
class CategoryIconUi(
    val imageVector: ImageVector,
    val hue: (CategoryPalette) -> Color,
    @StringRes val labelRes: Int
)

// The only place an icon is tied to a vector and a hue. No icon uses the red hue: red is kept
// for errors and deleting.
private fun uiOf(icon: CategoryIcon): CategoryIconUi = when (icon) {
    CategoryIcon.RESTAURANT ->
        CategoryIconUi(Icons.Rounded.Restaurant, CategoryPalette::deepOrange, R.string.category_icon_restaurant)
    CategoryIcon.SHOPPING_BASKET ->
        CategoryIconUi(Icons.Rounded.ShoppingBasket, CategoryPalette::lightGreen, R.string.category_icon_shopping_basket)
    CategoryIcon.SHOPPING_BAG ->
        CategoryIconUi(Icons.Rounded.ShoppingBag, CategoryPalette::pink, R.string.category_icon_shopping_bag)
    CategoryIcon.STOREFRONT ->
        CategoryIconUi(Icons.Rounded.Storefront, CategoryPalette::blue, R.string.category_icon_storefront)
    CategoryIcon.LOCAL_CAFE ->
        CategoryIconUi(Icons.Rounded.LocalCafe, CategoryPalette::brown, R.string.category_icon_local_cafe)
    CategoryIcon.FASTFOOD ->
        CategoryIconUi(Icons.Rounded.Fastfood, CategoryPalette::amber, R.string.category_icon_fastfood)
    CategoryIcon.LOCAL_PIZZA ->
        CategoryIconUi(Icons.Rounded.LocalPizza, CategoryPalette::deepOrange, R.string.category_icon_local_pizza)
    CategoryIcon.LOCAL_BAR ->
        CategoryIconUi(Icons.Rounded.LocalBar, CategoryPalette::purple, R.string.category_icon_local_bar)
    CategoryIcon.CHECKROOM ->
        CategoryIconUi(Icons.Rounded.Checkroom, CategoryPalette::indigo, R.string.category_icon_checkroom)
    CategoryIcon.DEVICES ->
        CategoryIconUi(Icons.Rounded.Devices, CategoryPalette::blueGrey, R.string.category_icon_devices)
    CategoryIcon.RECEIPT_LONG ->
        CategoryIconUi(Icons.AutoMirrored.Rounded.ReceiptLong, CategoryPalette::blue, R.string.category_icon_receipt_long)
    CategoryIcon.HOME ->
        CategoryIconUi(Icons.Rounded.Home, CategoryPalette::brown, R.string.category_icon_home)
    CategoryIcon.SUBSCRIPTIONS ->
        CategoryIconUi(Icons.Rounded.Subscriptions, CategoryPalette::deepIndigo, R.string.category_icon_subscriptions)
    CategoryIcon.APARTMENT ->
        CategoryIconUi(Icons.Rounded.Apartment, CategoryPalette::deepIndigo, R.string.category_icon_apartment)
    CategoryIcon.BOLT ->
        CategoryIconUi(Icons.Rounded.Bolt, CategoryPalette::amber, R.string.category_icon_bolt)
    CategoryIcon.WATER_DROP ->
        CategoryIconUi(Icons.Rounded.WaterDrop, CategoryPalette::cyan, R.string.category_icon_water_drop)
    CategoryIcon.WIFI ->
        CategoryIconUi(Icons.Rounded.Wifi, CategoryPalette::teal, R.string.category_icon_wifi)
    CategoryIcon.PHONE_ANDROID ->
        CategoryIconUi(Icons.Rounded.PhoneAndroid, CategoryPalette::indigo, R.string.category_icon_phone_android)
    CategoryIcon.LOCAL_LAUNDRY_SERVICE ->
        CategoryIconUi(Icons.Rounded.LocalLaundryService, CategoryPalette::blue, R.string.category_icon_local_laundry_service)
    CategoryIcon.BUILD ->
        CategoryIconUi(Icons.Rounded.Build, CategoryPalette::blueGrey, R.string.category_icon_build)
    CategoryIcon.DIRECTIONS_CAR ->
        CategoryIconUi(Icons.Rounded.DirectionsCar, CategoryPalette::teal, R.string.category_icon_directions_car)
    CategoryIcon.LOCAL_GAS_STATION ->
        CategoryIconUi(Icons.Rounded.LocalGasStation, CategoryPalette::amber, R.string.category_icon_local_gas_station)
    CategoryIcon.FLIGHT ->
        CategoryIconUi(Icons.Rounded.Flight, CategoryPalette::cyan, R.string.category_icon_flight)
    CategoryIcon.DIRECTIONS_BUS ->
        CategoryIconUi(Icons.Rounded.DirectionsBus, CategoryPalette::green, R.string.category_icon_directions_bus)
    CategoryIcon.TRAIN ->
        CategoryIconUi(Icons.Rounded.Train, CategoryPalette::indigo, R.string.category_icon_train)
    CategoryIcon.TWO_WHEELER ->
        CategoryIconUi(Icons.Rounded.TwoWheeler, CategoryPalette::deepOrange, R.string.category_icon_two_wheeler)
    CategoryIcon.LOCAL_TAXI ->
        CategoryIconUi(Icons.Rounded.LocalTaxi, CategoryPalette::amber, R.string.category_icon_local_taxi)
    CategoryIcon.DIRECTIONS_BIKE ->
        CategoryIconUi(Icons.AutoMirrored.Rounded.DirectionsBike, CategoryPalette::lightGreen, R.string.category_icon_directions_bike)
    CategoryIcon.HOTEL ->
        CategoryIconUi(Icons.Rounded.Hotel, CategoryPalette::purple, R.string.category_icon_hotel)
    CategoryIcon.MEDICAL_SERVICES ->
        CategoryIconUi(Icons.Rounded.MedicalServices, CategoryPalette::green, R.string.category_icon_medical_services)
    CategoryIcon.SCHOOL ->
        CategoryIconUi(Icons.Rounded.School, CategoryPalette::indigo, R.string.category_icon_school)
    CategoryIcon.MOVIE ->
        CategoryIconUi(Icons.Rounded.Movie, CategoryPalette::purple, R.string.category_icon_movie)
    CategoryIcon.SPA ->
        CategoryIconUi(Icons.Rounded.Spa, CategoryPalette::pink, R.string.category_icon_spa)
    CategoryIcon.CARD_GIFTCARD ->
        CategoryIconUi(Icons.Rounded.CardGiftcard, CategoryPalette::amber, R.string.category_icon_card_giftcard)
    CategoryIcon.FITNESS_CENTER ->
        CategoryIconUi(Icons.Rounded.FitnessCenter, CategoryPalette::lightGreen, R.string.category_icon_fitness_center)
    CategoryIcon.PETS ->
        CategoryIconUi(Icons.Rounded.Pets, CategoryPalette::amber, R.string.category_icon_pets)
    CategoryIcon.CHILD_CARE ->
        CategoryIconUi(Icons.Rounded.ChildCare, CategoryPalette::pink, R.string.category_icon_child_care)
    CategoryIcon.MENU_BOOK ->
        CategoryIconUi(Icons.AutoMirrored.Rounded.MenuBook, CategoryPalette::brown, R.string.category_icon_menu_book)
    CategoryIcon.SPORTS_ESPORTS ->
        CategoryIconUi(Icons.Rounded.SportsEsports, CategoryPalette::deepIndigo, R.string.category_icon_sports_esports)
    CategoryIcon.VOLUNTEER_ACTIVISM ->
        CategoryIconUi(Icons.Rounded.VolunteerActivism, CategoryPalette::pink, R.string.category_icon_volunteer_activism)
    CategoryIcon.CATEGORY ->
        CategoryIconUi(Icons.Rounded.Category, CategoryPalette::blueGrey, R.string.category_icon_category)
    CategoryIcon.PAYMENTS ->
        CategoryIconUi(Icons.Rounded.Payments, CategoryPalette::green, R.string.category_icon_payments)
    CategoryIcon.LAPTOP ->
        CategoryIconUi(Icons.Rounded.Laptop, CategoryPalette::teal, R.string.category_icon_laptop)
    CategoryIcon.TRENDING_UP ->
        CategoryIconUi(Icons.AutoMirrored.Rounded.TrendingUp, CategoryPalette::indigo, R.string.category_icon_trending_up)
    CategoryIcon.SAVINGS ->
        CategoryIconUi(Icons.Rounded.Savings, CategoryPalette::cyan, R.string.category_icon_savings)
    CategoryIcon.CURRENCY_EXCHANGE ->
        CategoryIconUi(Icons.Rounded.CurrencyExchange, CategoryPalette::lightGreen, R.string.category_icon_currency_exchange)
    CategoryIcon.REDEEM ->
        CategoryIconUi(Icons.Rounded.Redeem, CategoryPalette::purple, R.string.category_icon_redeem)
    CategoryIcon.ACCOUNT_BALANCE_WALLET ->
        CategoryIconUi(Icons.Rounded.AccountBalanceWallet, CategoryPalette::blueGrey, R.string.category_icon_account_balance_wallet)
    CategoryIcon.CREDIT_CARD ->
        CategoryIconUi(Icons.Rounded.CreditCard, CategoryPalette::blueGrey, R.string.category_icon_credit_card)
    CategoryIcon.ACCOUNT_BALANCE ->
        CategoryIconUi(Icons.Rounded.AccountBalance, CategoryPalette::blue, R.string.category_icon_account_balance)
    CategoryIcon.WORK ->
        CategoryIconUi(Icons.Rounded.Work, CategoryPalette::brown, R.string.category_icon_work)
    CategoryIcon.SHIELD ->
        CategoryIconUi(Icons.Rounded.Shield, CategoryPalette::teal, R.string.category_icon_shield)
    CategoryIcon.PERCENT ->
        CategoryIconUi(Icons.Rounded.Percent, CategoryPalette::deepOrange, R.string.category_icon_percent)
    CategoryIcon.CURRENCY_RUPEE ->
        CategoryIconUi(Icons.Rounded.CurrencyRupee, CategoryPalette::green, R.string.category_icon_currency_rupee)
    CategoryIcon.RECEIPT ->
        CategoryIconUi(Icons.Rounded.Receipt, CategoryPalette::deepIndigo, R.string.category_icon_receipt)
}

// Built once: the mapping is fixed and looked up for every row on screen
private val uiByIcon = CategoryIcon.entries.associateWith(::uiOf)

val CategoryIcon.ui: CategoryIconUi get() = uiByIcon.getValue(this)

/** The icon's identity color in the current theme. */
val CategoryIcon.color: Color
    @Composable
    @ReadOnlyComposable
    get() = ui.hue(MaterialTheme.extendedColors.categoryPalette)

@StringRes
fun CategoryIconGroup.labelRes(): Int = when (this) {
    CategoryIconGroup.FOOD_AND_SHOPPING -> R.string.category_icon_group_food_and_shopping
    CategoryIconGroup.HOME_AND_BILLS -> R.string.category_icon_group_home_and_bills
    CategoryIconGroup.GETTING_AROUND -> R.string.category_icon_group_getting_around
    CategoryIconGroup.HEALTH_AND_LIFE -> R.string.category_icon_group_health_and_life
    CategoryIconGroup.MONEY -> R.string.category_icon_group_money
}
