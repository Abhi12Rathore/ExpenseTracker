package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.utils.CurrencyFormatter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

@Composable
fun CategoryExpenseDonutChart(
    data: List<CategoryBreakdown>
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "Spending Analysis",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {

                Text(
                    text = "No expense data available",
                    modifier = Modifier.padding(32.dp)
                )

            } else {

                AndroidView(

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),

                    factory = { context ->
                        PieChart(context)
                    },

                    update = { chart ->

                        val entries = data.map {

                            PieEntry(
                                it.amount.toFloat(),
                                it.categoryName
                            )

                        }

                        val dataSet = PieDataSet(entries, "").apply {

                            sliceSpace = 4f

                            selectionShift = 10f

                            valueTextSize = 12f

                            valueTextColor = Color.WHITE

                            colors = listOf(

                                Color.parseColor("#4CAF50"), // Green

                                Color.parseColor("#F44336"), // Red

                                Color.parseColor("#2196F3"), // Blue

                                Color.parseColor("#FF9800"), // Orange

                                Color.parseColor("#9C27B0"), // Purple

                                Color.parseColor("#009688"), // Teal

                                Color.parseColor("#795548"), // Brown

                                Color.parseColor("#607D8B")  // Blue Grey

                            )

                        }

                        chart.data = PieData(dataSet)

                        //------------------------
                        // Donut
                        //------------------------

                        chart.isDrawHoleEnabled = true

                        chart.holeRadius = 62f

                        chart.transparentCircleRadius = 66f

                        chart.setHoleColor(Color.TRANSPARENT)

                        //------------------------
                        // Center Text
                        //------------------------

                        val totalExpense =
                            data.sumOf { it.amount }

                        chart.centerText =

                            "Expenses\n${CurrencyFormatter.format(totalExpense)}"

                        chart.setCenterTextSize(18f)

                        chart.setCenterTextColor(Color.DKGRAY)

                        //------------------------
                        // Description
                        //------------------------

                        chart.description.isEnabled = false

                        //------------------------
                        // Legend
                        //------------------------

                        chart.legend.apply {

                            isEnabled = true

                            orientation =
                                Legend.LegendOrientation.VERTICAL

                            verticalAlignment =
                                Legend.LegendVerticalAlignment.CENTER

                            horizontalAlignment =
                                Legend.LegendHorizontalAlignment.RIGHT

                            textSize = 12f

                            formSize = 12f

                            xEntrySpace = 8f

                            yEntrySpace = 6f

                            isWordWrapEnabled = true

                        }

                        //------------------------
                        // Entry Labels
                        //------------------------

                        chart.setEntryLabelColor(Color.BLACK)

                        chart.setEntryLabelTextSize(11f)

                        //------------------------
                        // Interaction
                        //------------------------

                        chart.setUsePercentValues(true)

                        chart.isRotationEnabled = true

                        chart.isHighlightPerTapEnabled = true

                        //------------------------
                        // Animation
                        //------------------------

                        chart.animateY(1400)

                        chart.invalidate()

                    }

                )

            }

        }

    }

}