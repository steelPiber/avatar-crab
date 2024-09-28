package com.example.avatar_crab.presentation.dashboard

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.avatar_crab.R
import com.example.avatar_crab.data.ActivityData
import com.example.avatar_crab.presentation.CustomMarkerView
import com.example.avatar_crab.presentation.MainViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by activityViewModels()
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)

        setupPieChart(pieChart)
        setupBarChart(barChart)

        viewModel.activityData.observe(viewLifecycleOwner) { data ->
            if (data.isNotEmpty()) {
                updateCharts(data)
            }
        }

        // Load today's activity data
        viewModel.loadTodayActivityData()

        // Schedule data clear work at midnight
        scheduleDataClearWork(requireContext())
    }

    private fun setupPieChart(pieChart: PieChart) {
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setUsePercentValues(true)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.centerText = "활동 상태"
        pieChart.setCenterTextSize(24f)
        pieChart.description.isEnabled = false

        val legend = pieChart.legend
        legend.isEnabled = false // Disable legend

        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: Highlight?) {
                if (e is PieEntry) {
                    pieChart.centerText = e.label
                }
            }

            override fun onNothingSelected() {
                pieChart.centerText = "활동 상태"
            }
        })
    }

    private fun setupBarChart(barChart: BarChart) {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 1500f // Set Y-axis maximum value to 1500
        barChart.xAxis.labelCount = 24
        barChart.xAxis.granularity = 1f
        barChart.setVisibleXRangeMaximum(24f)
        barChart.setVisibleXRangeMinimum(24f)
        barChart.isScaleXEnabled = true
        barChart.isScaleYEnabled = true
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter((0..23).map { it.toString().padStart(2, '0') })

        // Set custom marker view
        val markerView = CustomMarkerView(requireContext(), R.layout.custom_marker_view)
        markerView.chartView = barChart
        barChart.marker = markerView

        // Adjust legend size and text size
        val legend = barChart.legend
        legend.formSize = 12f
        legend.textSize = 14f
        barChart.data?.setDrawValues(false)
    }

    private fun updateCharts(data: List<ActivityData>) {
        updatePieChart(pieChart, data)
        updateBarChart(barChart, data)
        viewModel.updateChallengesWithActivityData(data)
    }

    private fun updatePieChart(pieChart: PieChart, data: List<ActivityData>) {
        val normalCount = data.count { it.tag == "normal" }
        val activeCount = data.count { it.tag == "active" }
        val exerciseCount = data.count { it.tag == "exercise" }
        val restCount = data.count { it.tag == "rest" }
        val totalCount = normalCount + activeCount + exerciseCount + restCount

        if (totalCount == 0) {
            return
        }

        val normalPercentage = (normalCount.toFloat() / totalCount) * 100
        val activePercentage = (activeCount.toFloat() / totalCount) * 100
        val exercisePercentage = (exerciseCount.toFloat() / totalCount) * 100
        val restPercentage = (restCount.toFloat() / totalCount) * 100

        val entries = listOf(
            PieEntry(normalPercentage, "보통"),
            PieEntry(activePercentage, "활동"),
            PieEntry(exercisePercentage, "운동"),
            PieEntry(restPercentage, "휴식")
        )

        val pastelColors = listOf(
            Color.parseColor("#FFB3BA"), // Light Pink
            Color.parseColor("#FFDFBA"), // Light Orange
            Color.parseColor("#BAE1FF"), // Light Blue
            Color.parseColor("#BAFFC9")  // Light Green
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = pastelColors
            valueTextColor = Color.BLACK
            valueTextSize = 20f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}%"
                }
            }
        }

        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    private fun updateBarChart(barChart: BarChart, data: List<ActivityData>) {
        val hourFormat = SimpleDateFormat("HH", Locale.getDefault())

        val groupedData = data.groupBy {
            val date = Date(it.timestamp)
            hourFormat.format(date)
        }

        val entries = mutableListOf<BarEntry>()
        val labels = (0..23).map { it.toString().padStart(2, '0') }.toMutableList()

        for ((index, hour) in labels.withIndex()) {
            val normalCount = groupedData[hour]?.count { it.tag == "normal" } ?: 0
            val activeCount = groupedData[hour]?.count { it.tag == "active" } ?: 0
            val exerciseCount = groupedData[hour]?.count { it.tag == "exercise" } ?: 0
            val restCount = groupedData[hour]?.count { it.tag == "rest" } ?: 0
            if (normalCount > 0 || activeCount > 0 || exerciseCount > 0 || restCount > 0) {
                val entry = BarEntry(index.toFloat(), floatArrayOf(normalCount.toFloat(), activeCount.toFloat(), exerciseCount.toFloat(), restCount.toFloat()))
                entry.data = listOf(normalCount.toFloat(), activeCount.toFloat(), exerciseCount.toFloat(), restCount.toFloat()) // Set data for marker view
                entries.add(entry)
            }
        }

        val pastelColors = listOf(
            Color.parseColor("#FFB3BA"), // Light Pink
            Color.parseColor("#FFDFBA"), // Light Orange
            Color.parseColor("#BAE1FF"), // Light Blue
            Color.parseColor("#BAFFC9")  // Light Green
        )

        val dataSet = BarDataSet(entries, "").apply {
            setColors(pastelColors)
            stackLabels = arrayOf("보통", "활동", "운동", "휴식")
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.4f
        barData.setDrawValues(false)
        barChart.data = barData

        barChart.setFitBars(true)
        barChart.setVisibleXRangeMaximum(6f)
        barChart.setVisibleXRangeMinimum(1f)

        barChart.invalidate()
    }

    private fun scheduleDataClearWork(context: Context) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        dueDate.set(Calendar.HOUR_OF_DAY, 0)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val workRequest = OneTimeWorkRequestBuilder<ClearDataWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
