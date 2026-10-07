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
import androidx.compose.ui.graphics.Color

object CategoryIcons {
    val iconColors = mapOf(
        "Shopping" to Color(0xFF5C6BC0),      // Indigo
        "Food" to Color(0xFFFF7043),          // Deep Orange
        "Transport" to Color(0xFF26A69A),     // Teal
        "Bills" to Color(0xFFEF5350),         // Red
        "Health" to Color(0xFF66BB6A),        // Green
        "Education" to Color(0xFF42A5F5),     // Blue
        "Entertainment" to Color(0xFFAB47BC), // Purple
        "Gift" to Color(0xFFEC407A),          // Pink
        "Home" to Color(0xFF795548),          // Brown
        "Work" to Color(0xFF607D8B),          // Blue Grey
        "Travel" to Color(0xFF00BCD4),        // Cyan
        "Pets" to Color(0xFFFFB300),          // Amber
        "Subscriptions" to Color(0xFF3F51B5), // Indigo Dark
        "Fitness" to Color(0xFF8BC34A),
        "Payments" to Color(0xFFEF5350),
        "Work" to Color(0xFF42A5F5),
        "TrendingUp" to Color(0xFF795548),
        "RealEstateAgent" to Color(0xFFEF5350),
        "AccountBalance" to Color(0xFFEC407A),
        "Savings" to Color(0xFF607D8B),
    )

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