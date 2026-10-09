package com.example.expensetrackerpro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The hues that give each category its identity color. */
@Immutable
data class CategoryPalette(
    val indigo: Color,
    val deepOrange: Color,
    val teal: Color,
    val red: Color,
    val green: Color,
    val blue: Color,
    val purple: Color,
    val pink: Color,
    val brown: Color,
    val blueGrey: Color,
    val cyan: Color,
    val amber: Color,
    val deepIndigo: Color,
    val lightGreen: Color
) {
    /** Distinct hues for chart series, in the order they are assigned. */
    val series: List<Color>
        get() = listOf(green, red, blue, deepOrange, purple, teal, brown, blueGrey)
}

/** App-specific colors that have no Material 3 role. */
@Immutable
data class ExtendedColors(
    val income: Color,
    val expense: Color,
    val chartGrid: Color,
    val chartAxisText: Color,
    val categoryPalette: CategoryPalette
)

val LightExtendedColors = ExtendedColors(
    income = IncomeLight,
    expense = ExpenseLight,
    chartGrid = ChartGridLight,
    chartAxisText = ChartAxisTextLight,
    categoryPalette = CategoryPalette(
        indigo = CategoryIndigoLight,
        deepOrange = CategoryDeepOrangeLight,
        teal = CategoryTealLight,
        red = CategoryRedLight,
        green = CategoryGreenLight,
        blue = CategoryBlueLight,
        purple = CategoryPurpleLight,
        pink = CategoryPinkLight,
        brown = CategoryBrownLight,
        blueGrey = CategoryBlueGreyLight,
        cyan = CategoryCyanLight,
        amber = CategoryAmberLight,
        deepIndigo = CategoryDeepIndigoLight,
        lightGreen = CategoryLightGreenLight
    )
)

val DarkExtendedColors = ExtendedColors(
    income = IncomeDark,
    expense = ExpenseDark,
    chartGrid = ChartGridDark,
    chartAxisText = ChartAxisTextDark,
    categoryPalette = CategoryPalette(
        indigo = CategoryIndigoDark,
        deepOrange = CategoryDeepOrangeDark,
        teal = CategoryTealDark,
        red = CategoryRedDark,
        green = CategoryGreenDark,
        blue = CategoryBlueDark,
        purple = CategoryPurpleDark,
        pink = CategoryPinkDark,
        brown = CategoryBrownDark,
        blueGrey = CategoryBlueGreyDark,
        cyan = CategoryCyanDark,
        amber = CategoryAmberDark,
        deepIndigo = CategoryDeepIndigoDark,
        lightGreen = CategoryLightGreenDark
    )
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
