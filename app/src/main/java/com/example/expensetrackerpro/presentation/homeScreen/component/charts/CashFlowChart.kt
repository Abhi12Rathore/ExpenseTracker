package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                text = "Cash Flow Trend",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {

                Text(
                    text = "No transaction data available",
                    modifier = Modifier.padding(32.dp)
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

                                color = Color.parseColor("#4CAF50")

                                valueTextColor =
                                    Color.parseColor("#4CAF50")

                                lineWidth = 3f

                                setCircleColor(Color.parseColor("#4CAF50"))

                                circleRadius = 4f

                                setDrawCircleHole(false)

                                mode =
                                    LineDataSet.Mode.CUBIC_BEZIER

                                setDrawFilled(true)

                                fillColor =
                                    Color.parseColor("#A5D6A7")

                            }

                        //--------------------------------------------------
                        // Expense Line
                        //--------------------------------------------------

                        val expenseDataSet =
                            LineDataSet(
                                expenseEntries,
                                "Expense"
                            ).apply {

                                color = Color.parseColor("#F44336")

                                valueTextColor =
                                    Color.parseColor("#F44336")

                                lineWidth = 3f

                                setCircleColor(Color.parseColor("#F44336"))

                                circleRadius = 4f

                                setDrawCircleHole(false)

                                mode =
                                    LineDataSet.Mode.CUBIC_BEZIER

                                setDrawFilled(true)

                                fillColor =
                                    Color.parseColor("#EF9A9A")

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

                        }

                        //--------------------------------------------------
                        // Left Axis
                        //--------------------------------------------------

                        chart.axisLeft.apply {

                            setDrawGridLines(true)

                            axisMinimum = 0f

                        }

                        chart.axisRight.isEnabled = false

                        //--------------------------------------------------
                        // Legend
                        //--------------------------------------------------

                        chart.legend.apply {

                            isEnabled = true

                            textSize = 12f

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