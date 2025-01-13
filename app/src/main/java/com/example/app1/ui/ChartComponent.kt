package com.example.app1.ui

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import com.example.app1.data.HistoricalRate
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ExchangeRateChart(
    historicalRates: List<HistoricalRate>,
    modifier: Modifier = Modifier,
    onPointSelected: (String, Double) -> Unit // Callback for point selection
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(false)
                
                // Configure X-axis
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = Color.parseColor("#9E9E9E")  // Lighter gray for dark background
                    granularity = 1f
                }
                
                // Configure Y-axis
                axisLeft.apply {
                    textColor = Color.parseColor("#9E9E9E")  // Lighter gray for dark background
                    setDrawGridLines(true)
                    gridColor = Color.parseColor("#424242")  // Darker grid lines
                }
                axisRight.isEnabled = false
                
                // Disable legend
                legend.isEnabled = false
                
                // Set dark background
                setBackgroundColor(Color.parseColor("#1E1E1E"))  // Dark background
                setDrawGridBackground(false)
                
                // Remove border
                setDrawBorders(false)
                
                // Set a listener for point selection
                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        e?.let {
                            val date = historicalRates.getOrNull(it.x.toInt())?.effectiveDate ?: "Unknown Date"
                            val value = it.y.toDouble()
                            onPointSelected(date, value) // Trigger the callback
                        }
                    }

                    override fun onNothingSelected() {
                        // Handle case when nothing is selected
                    }
                })
            }
        },
        update = { chart ->
            val entries = historicalRates.mapIndexed { index, rate ->
                Entry(index.toFloat(), rate.mid.toFloat())
            }

            val dataSet = LineDataSet(entries, "Exchange Rate").apply {
                color = Color.parseColor("#4CAF50")  // Green color
                setCircleColor(Color.parseColor("#4CAF50"))
                setDrawValues(false)
                lineWidth = 1.5f
                circleRadius = 3f
                mode = LineDataSet.Mode.CUBIC_BEZIER  // Smooth curve
                cubicIntensity = 0.2f  // Less curved
                setDrawFilled(true)
                fillColor = Color.parseColor("#4CAF50")
                fillAlpha = 20  // Semi-transparent fill
                setDrawCircles(false)  // Remove points
                setDrawHorizontalHighlightIndicator(false)  // Only vertical highlight
                highLightColor = Color.parseColor("#4CAF50")  // Highlight color
                setDrawVerticalHighlightIndicator(true)
            }

            chart.data = LineData(dataSet)
            
            // Format dates for X-axis
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(
                historicalRates.map { dateFormat.parse(it.effectiveDate)?.let { 
                    SimpleDateFormat("MM-dd", Locale.getDefault()).format(it) 
                } ?: "" }
            )
            
            // Adjust viewport to show slight padding
            chart.setVisibleXRangeMaximum(historicalRates.size.toFloat())
            chart.moveViewToX(historicalRates.size.toFloat())
            
            chart.invalidate()
        }
    )
} 