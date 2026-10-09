package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun MonthlyTrendBarChart(
    data: List<MonthlyTrend>
) {
    val colors = chartColors()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "Monthly Overview",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {

                Text(
                    text = "No monthly data available",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )

            } else {

                AndroidView(

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),

                    factory = { context ->
                        BarChart(context)
                    },

                    update = { chart ->

                        val incomeEntries =
                            data.mapIndexed { index, item ->
                                BarEntry(
                                    index.toFloat(),
                                    item.income.toFloat()
                                )
                            }

                        val expenseEntries =
                            data.mapIndexed { index, item ->
                                BarEntry(
                                    index.toFloat(),
                                    item.expense.toFloat()
                                )
                            }

                        val incomeSet = BarDataSet(
                            incomeEntries,
                            "Income"
                        ).apply {

                            color = colors.income
                            valueTextColor = colors.income

                            valueTextSize = 10f

                        }

                        val expenseSet = BarDataSet(
                            expenseEntries,
                            "Expense"
                        ).apply {

                            color = colors.expense
                            valueTextColor = colors.expense

                            valueTextSize = 10f

                        }

                        val barData = BarData(
                            incomeSet,
                            expenseSet
                        )

                        barData.barWidth = 0.35f

                        chart.data = barData

                        //----------------------------
                        // X Axis
                        //----------------------------

                        val months = data.map {
                            "${it.month}/${it.year}"
                        }

                        chart.xAxis.apply {

                            position = XAxis.XAxisPosition.BOTTOM

                            valueFormatter =
                                IndexAxisValueFormatter(months)

                            granularity = 1f

                            setDrawGridLines(false)

                            axisMinimum = 0f

                            textColor = colors.axisText

                            axisLineColor = colors.grid

                        }

                        //----------------------------
                        // Group Bars
                        //----------------------------

                        val groupSpace = 0.30f
                        val barSpace = 0.05f

                        chart.groupBars(
                            0f,
                            groupSpace,
                            barSpace
                        )

                        chart.xAxis.axisMaximum =
                            0f + chart.barData.getGroupWidth(
                                groupSpace,
                                barSpace
                            ) * data.size

                        //----------------------------
                        // Y Axis
                        //----------------------------

                        chart.axisLeft.apply {

                            setDrawGridLines(false)

                            textColor = colors.axisText

                            axisLineColor = colors.grid

                        }

                        chart.axisRight.isEnabled = false

                        //----------------------------
                        // Legend
                        //----------------------------

                        chart.legend.apply {

                            isEnabled = true

                            textSize = 12f

                            textColor = colors.axisText

                            verticalAlignment =
                                Legend.LegendVerticalAlignment.TOP

                            horizontalAlignment =
                                Legend.LegendHorizontalAlignment.RIGHT

                        }

                        //----------------------------
                        // Description
                        //----------------------------

                        chart.description.isEnabled = false

                        //----------------------------
                        // Interaction
                        //----------------------------

                        chart.setFitBars(false)

                        chart.setPinchZoom(false)

                        chart.isDoubleTapToZoomEnabled = false

                        chart.setScaleEnabled(false)

                        //----------------------------
                        // Animation
                        //----------------------------

                        chart.animateXY(
                            1200,
                            1200
                        )

                        chart.invalidate()

                    }

                )

            }

        }

    }

}