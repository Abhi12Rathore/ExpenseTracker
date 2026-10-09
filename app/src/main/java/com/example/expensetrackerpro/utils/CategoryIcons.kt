package com.example.expensetrackerpro.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RealEstateAgent
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.example.expensetrackerpro.ui.theme.CategoryPalette
import com.example.expensetrackerpro.ui.theme.extendedColors

object CategoryIcons {
    // Which hue of the theme's category palette each icon uses
    private val iconHues: Map<String, (CategoryPalette) -> Color> = mapOf(
        "Shopping" to CategoryPalette::indigo,
        "Food" to CategoryPalette::deepOrange,
        "Transport" to CategoryPalette::teal,
        "Bills" to CategoryPalette::red,
        "Health" to CategoryPalette::green,
        "Education" to CategoryPalette::blue,
        "Entertainment" to CategoryPalette::purple,
        "Gift" to CategoryPalette::pink,
        "Home" to CategoryPalette::brown,
        "Travel" to CategoryPalette::cyan,
        "Pets" to CategoryPalette::amber,
        "Subscriptions" to CategoryPalette::deepIndigo,
        "Fitness" to CategoryPalette::lightGreen,
        "Payments" to CategoryPalette::red,
        "Work" to CategoryPalette::blue,
        "TrendingUp" to CategoryPalette::brown,
        "RealEstateAgent" to CategoryPalette::red,
        "AccountBalance" to CategoryPalette::pink,
        "Savings" to CategoryPalette::blueGrey,
    )

    /** The icon's identity color in the current theme, or null when the icon has none. */
    @Composable
    @ReadOnlyComposable
    fun color(iconName: String): Color? =
        iconHues[iconName]?.invoke(MaterialTheme.extendedColors.categoryPalette)

    val icons = mapOf(
        "Shopping" to Icons.Default.ShoppingCart,
        "Food" to Icons.Default.Restaurant,
        "Transport" to Icons.Default.DirectionsCar,
        "Bills" to Icons.Default.Receipt,
        "Health" to Icons.Default.MedicalServices,
        "Education" to Icons.Default.School,
        "Entertainment" to Icons.Default.Movie,
        "Gift" to Icons.Default.CardGiftcard,
        "Home" to Icons.Default.Home,
        "Work" to Icons.Default.Work,
        "Travel" to Icons.Default.Flight,
        "Pets" to Icons.Default.Pets,
        "Subscriptions" to Icons.Default.Subscriptions,
        "Fitness" to Icons.Default.FitnessCenter,
        "Investments" to Icons.Default.MonetizationOn,

        "Salary" to Icons.Default.Payments,
        "Payments" to Icons.Default.Payments,
        "Work" to Icons.Default.Work,
        "TrendingUp" to Icons.Default.TrendingUp,
        "RealEstateAgent" to Icons.Default.RealEstateAgent,
        "AccountBalance" to Icons.Default.AccountBalance,
        "Savings" to Icons.Default.Savings,
    )
}