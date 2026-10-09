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
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun CashFlowChart(
    data: List<CashFlowPoint>
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
                text = "Cash Flow Trend",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {

                Text(
                    text = "No transaction data available",
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
                        LineChart(context)
                    },

                    update = { chart ->

                        val incomeEntries =
                            data.mapIndexed { index, item ->
                                Entry(
                                    index.toFloat(),
                                    item.income.toFloat()
                                )
                            }

                        val expenseEntries =
                            data.mapIndexed { index, item ->
                                Entry(
                                    index.toFloat(),
                                    item.expense.toFloat()
                                )
                            }

                        //--------------------------------------------------
                        // Income Line
                        //--------------------------------------------------

                        val incomeDataSet =
                            LineDataSet(
                                incomeEntries,
                                "Income"
                            ).apply {

                                color = colors.income

                                valueTextColor = colors.income

                                lineWidth = 3f

                                setCircleColor(colors.income)

                                circleRadius = 4f

                                setDrawCircleHole(false)

                                mode =
                                    LineDataSet.Mode.CUBIC_BEZIER

                                setDrawFilled(true)

                                fillColor = colors.income

                            }

                        //--------------------------------------------------
                        // Expense Line
                        //--------------------------------------------------

                        val expenseDataSet =
                            LineDataSet(
                                expenseEntries,
                                "Expense"
                            ).apply {

                                color = colors.expense

                                valueTextColor = colors.expense

                                lineWidth = 3f

                                setCircleColor(colors.expense)

                                circleRadius = 4f

                                setDrawCircleHole(false)

                                mode =
                                    LineDataSet.Mode.CUBIC_BEZIER

                                setDrawFilled(true)

                                fillColor = colors.expense

                            }

                        chart.data =
                            LineData(
                                incomeDataSet,
                                expenseDataSet
                            )

                        //--------------------------------------------------
                        // X Axis
                        //--------------------------------------------------

                        val labels =
                            data.mapIndexed { index, _ ->
                                "${index + 1}"
                            }

                        chart.xAxis.apply {

                            position =
                                XAxis.XAxisPosition.BOTTOM

                            valueFormatter =
                                IndexAxisValueFormatter(labels)

                            granularity = 1f

                            setDrawGridLines(false)

                            textColor = colors.axisText

                            axisLineColor = colors.grid

                        }

                        //--------------------------------------------------
                        // Left Axis
                        //--------------------------------------------------

                        chart.axisLeft.apply {

                            setDrawGridLines(true)

                            axisMinimum = 0f

                            textColor = colors.axisText

                            gridColor = colors.grid

                            axisLineColor = colors.grid

                        }

                        chart.axisRight.isEnabled = false

                        //--------------------------------------------------
                        // Legend
                        //--------------------------------------------------

                        chart.legend.apply {

                            isEnabled = true

                            textSize = 12f

                            textColor = colors.axisText

                            verticalAlignment =
                                Legend.LegendVerticalAlignment.TOP

                            horizontalAlignment =
                                Legend.LegendHorizontalAlignment.RIGHT

                        }

                        //--------------------------------------------------
                        // Description
                        //--------------------------------------------------

                        chart.description.isEnabled = false

                        //--------------------------------------------------
                        // Interaction
                        //--------------------------------------------------

                        chart.setPinchZoom(false)

                        chart.setScaleEnabled(false)

                        chart.isDoubleTapToZoomEnabled = false

                        chart.setTouchEnabled(true)

                        //--------------------------------------------------
                        // Animation
                        //--------------------------------------------------

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