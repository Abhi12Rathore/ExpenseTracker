package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.expensetrackerpro.ui.theme.extendedColors

/** Theme colors as ARGB ints, the form MPAndroidChart's View-based API takes. */
internal data class ChartColors(
    val income: Int,
    val expense: Int,
    val grid: Int,
    val axisText: Int,
    val centerText: Int,
    val sliceValue: Int,
    val sliceLabel: Int,
    val series: List<Int>,
    val transparent: Int
)

@Composable
@ReadOnlyComposable
internal fun chartColors(): ChartColors {
    val extended = MaterialTheme.extendedColors
    return ChartColors(
        income = extended.income.toArgb(),
        expense = extended.expense.toArgb(),
        grid = extended.chartGrid.toArgb(),
        axisText = extended.chartAxisText.toArgb(),
        centerText = MaterialTheme.colorScheme.onSurface.toArgb(),
        sliceValue = extended.chartSliceValue.toArgb(),
        sliceLabel = extended.chartSliceLabel.toArgb(),
        series = extended.categoryPalette.series.map { it.toArgb() },
        transparent = Color.Transparent.toArgb()
    )
}
