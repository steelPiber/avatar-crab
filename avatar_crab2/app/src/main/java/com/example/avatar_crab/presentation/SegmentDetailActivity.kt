package com.example.avatar_crab.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import piber.avatar_crab.R
import com.example.avatar_crab.data.exercise.LatLngEntity
import com.example.avatar_crab.data.exercise.SegmentDataEntity
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class SegmentDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var avgHeartRateTextView: TextView
    private lateinit var minHeartRateTextView: TextView
    private lateinit var maxHeartRateTextView: TextView
    private lateinit var lineChart: LineChart
    private lateinit var horizontalBarChart: HorizontalBarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segment_detail)

        avgHeartRateTextView = findViewById(R.id.tv_avg_heart_rate)
        minHeartRateTextView = findViewById(R.id.tv_min_heart_rate)
        maxHeartRateTextView = findViewById(R.id.tv_max_heart_rate)
        lineChart = findViewById(R.id.lineChart)
        horizontalBarChart = findViewById(R.id.horizontalBarChart)

        val avgHeartRate = intent.getFloatExtra("avgHeartRate", 0f)
        val minHeartRate = intent.getFloatExtra("minHeartRate", 0f)
        val maxHeartRate = intent.getFloatExtra("maxHeartRate", 0f)

        avgHeartRateTextView.text = "평균 심박수: $avgHeartRate"
        minHeartRateTextView.text = "최저 심박수: $minHeartRate"
        maxHeartRateTextView.text = "최고 심박수: $maxHeartRate"

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val segments = intent.getParcelableArrayListExtra<SegmentDataEntity>("segments") ?: emptyList()
        val pathPoints = intent.getParcelableArrayListExtra<LatLngEntity>("pathPoints") ?: emptyList()

        Log.d("SegmentDetailActivity", "Segments: $segments")
        Log.d("SegmentDetailActivity", "Path Points: $pathPoints")
        setupLineChart(segments)
        setupHorizontalBarChart(segments, avgHeartRate)

        val hrZonesLayout: LinearLayout = findViewById(R.id.hr_zones_layout)
        displayHeartRateZones(hrZonesLayout, avgHeartRate)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        displayPath()
    }

    private fun displayPath() {
        val pathPoints = intent.getParcelableArrayListExtra<LatLngEntity>("pathPoints") ?: emptyList()
        if (pathPoints.isNotEmpty()) {
            val latLngs = pathPoints.map { LatLng(it.latitude, it.longitude) }
            val polylineOptions = PolylineOptions().addAll(latLngs).width(30f).color(resources.getColor(R.color.colorAccent, null))
            mMap.addPolyline(polylineOptions)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.first(), 15f))

            val firstPoint = latLngs.first()
            val lastPoint = latLngs.last()
            mMap.addMarker(MarkerOptions().position(firstPoint).title("Start").icon(bitmapDescriptorFromVector(this, R.drawable.flag_start)))
            mMap.addMarker(MarkerOptions().position(lastPoint).title("End").icon(bitmapDescriptorFromVector(this, R.drawable.person_end)))
        }
    }

    private fun setupLineChart(segments: List<SegmentDataEntity>) {
        val entries = segments.mapIndexed { index, segment ->
            Entry(index.toFloat(), segment.avgHeartRate)
        }

        val lineDataSet = LineDataSet(entries, "구간심박수").apply {
            axisDependency = YAxis.AxisDependency.LEFT
            color = ContextCompat.getColor(this@SegmentDetailActivity, R.color.colorPrimary)
            valueTextColor = ContextCompat.getColor(this@SegmentDetailActivity, R.color.colorPrimaryDark)
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            setDrawCircles(true)
            setDrawValues(true)
        }

        lineChart.data = LineData(lineDataSet)
        lineChart.description.text = "구간심박수"
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 1f
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisRight.isEnabled = false
        lineChart.invalidate()

        val mv = CustomMarkerView(this, R.layout.segment_detail_marker_view)
        lineChart.marker = mv
    }

    private fun setupHorizontalBarChart(segments: List<SegmentDataEntity>, avgHeartRate: Float) {
        val zoneCounts = IntArray(5)

        for (segment in segments) {
            when (segment.avgHeartRate) {
                in 0.5 * avgHeartRate..0.6 * avgHeartRate -> zoneCounts[0]++
                in 0.6 * avgHeartRate..0.7 * avgHeartRate -> zoneCounts[1]++
                in 0.7 * avgHeartRate..0.8 * avgHeartRate -> zoneCounts[2]++
                in 0.8 * avgHeartRate..0.9 * avgHeartRate -> zoneCounts[3]++
                in 0.9 * avgHeartRate..1.0 * avgHeartRate -> zoneCounts[4]++
            }
        }

        val barEntries = zoneCounts.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val barDataSet = BarDataSet(barEntries, "심박수 운동 영역").apply {
            color = ContextCompat.getColor(this@SegmentDetailActivity, R.color.colorPrimary)
            valueTextColor = ContextCompat.getColor(this@SegmentDetailActivity, R.color.colorPrimaryDark)
        }

        horizontalBarChart.data = BarData(barDataSet)
        horizontalBarChart.description.text = "심박수 운동 영역"
        horizontalBarChart.description.textSize = 12f

        val xAxis = horizontalBarChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return when (value.toInt()) {
                        0 -> "zone1"
                        1 -> "zone2"
                        2 -> "zone3"
                        3 -> "zone4"
                        4 -> "zone5"
                        else -> ""
                    }
                }
            }
            position = XAxis.XAxisPosition.BOTTOM
        }

        horizontalBarChart.axisLeft.axisMinimum = 0f
        horizontalBarChart.axisRight.isEnabled = false
        horizontalBarChart.invalidate()

        horizontalBarChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    Toast.makeText(this@SegmentDetailActivity, "Zone: ${xAxis.valueFormatter.getFormattedValue(it.x)}, Count: ${it.y}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected() {}
        })
    }

    private fun displayHeartRateZones(hrZonesLayout: LinearLayout, avgHeartRate: Float) {
        val zones = listOf(
            Zone("100% 레드 라인 존, VO2 최대", 1.0, "#FF0000"),
            Zone("90% 무산소 운동 존", 0.9, "#FF4500"),
            Zone("80% 유산소 운동 존", 0.8, "#FFA500"),
            Zone("70% 회복 존", 0.7, "#FFD700"),
            Zone("60% 건강한 심장 존", 0.6, "#ADFF2F")
        )

        zones.forEach { zone ->
            val zoneTextView = TextView(this).apply {
                text = "${zone.name}: ${calculateHeartRate(avgHeartRate, zone.percentage)} BPM"
                setBackgroundColor(android.graphics.Color.parseColor(zone.color))
                setPadding(8, 8, 8, 8)
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
            }
            hrZonesLayout.addView(zoneTextView)
        }
    }

    private fun calculateHeartRate(avgHeartRate: Float, percentage: Double): Int {
        return (avgHeartRate * percentage).toInt()
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable?.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
        private val tvContent: TextView = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            e?.let {
                tvContent.text = "Distance: ${it.x * 100}m\nBPM: ${it.y}"
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2).toFloat(), -height.toFloat())
        }
    }

    data class Zone(val name: String, val percentage: Double, val color: String)
}
