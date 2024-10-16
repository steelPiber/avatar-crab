package com.example.avatar_crab.presentation.exercise

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import piber.avatar_crab
    .R
import com.example.avatar_crab.data.ActivityData
import com.example.avatar_crab.data.exercise.*
import com.example.avatar_crab.presentation.MainViewModel
import com.example.avatar_crab.presentation.SegmentDetailActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.roundToInt

class ExerciseFragment : Fragment(), OnMapReadyCallback, SensorEventListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var gyroscope: Sensor

    private var tracking = false
    private var stepCount = 0
    private var currentPace = 0.0f

    private lateinit var startButton: Button
    private lateinit var tvKm: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvAvgHeartRate: TextView
    private lateinit var heartRateZoneChart: BarChart
    private lateinit var segmentHeartRateChart: LineChart

    private var startTime: Long = 0
    private var totalDistance: Float = 0f
    private var totalCalories: Float = 0f
    private var pathPoints: MutableList<LatLng> = mutableListOf()
    private var heartRateReadings = mutableListOf<Int>()
    private var meterHeartRates = mutableListOf<SegmentDataEntity>()
    private var hundredMeterSegments: MutableList<SegmentDataEntity> = mutableListOf()

    private val viewModel: MainViewModel by activityViewModels()
    private val chartViewModel: ChartViewModel by activityViewModels()

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val TAG = "ExerciseFragment"
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val location = intent?.getParcelableExtra<Location>("location")
            val heartRate = intent?.getIntExtra("heartRate", 0)
            location?.let { updateUI(it) }
            heartRate?.let { updateHeartRate(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exercise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this) ?: run {
            Toast.makeText(requireContext(), "Error initializing map fragment", Toast.LENGTH_SHORT).show()
            return
        }

        startButton = view.findViewById(R.id.btn_start)
        tvKm = view.findViewById(R.id.tv_km)
        tvTime = view.findViewById(R.id.tv_time)
        tvCalories = view.findViewById(R.id.tv_calories)
        tvHeartRate = view.findViewById(R.id.tv_heart_rate)
        tvAvgHeartRate = view.findViewById(R.id.tv_avg_heart_rate)
        heartRateZoneChart = view.findViewById(R.id.heartRateZoneChart)
        segmentHeartRateChart = view.findViewById(R.id.segmentHeartRateChart)

        startButton.setOnClickListener {
            if (tracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }

        locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateUI(location)
                }
            }
        }

        checkLocationPermission()

        viewModel.heartRate.observe(viewLifecycleOwner) { heartRate ->
            tvHeartRate.text = "심박수: $heartRate"
            val heartRateValue = heartRate.removeSuffix(" BPM").toIntOrNull()
            heartRateValue?.let {
                heartRateReadings.add(it)
                viewModel.saveHeartRateData(it)
                if (tracking) {
                    val avgHeartRate = viewModel.dailyAverageHeartRate.value ?: 0
                    chartViewModel.updateHeartRateZones(meterHeartRates, avgHeartRate)
                }
            }
        }

        viewModel.dailyAverageHeartRate.observe(viewLifecycleOwner) { avgHeartRate ->
            tvAvgHeartRate.text = "하루 평균 심박수: $avgHeartRate BPM"
        }

        chartViewModel.heartRateZoneEntries.observe(viewLifecycleOwner) { entries ->
            Log.d(TAG, "Updating heart rate zone chart with entries: $entries")
            val dataSet = BarDataSet(entries, "Heart Rate Zones").apply {
                colors = listOf(
                    ContextCompat.getColor(requireContext(), R.color.zone1),
                    ContextCompat.getColor(requireContext(), R.color.zone2),
                    ContextCompat.getColor(requireContext(), R.color.zone3),
                    ContextCompat.getColor(requireContext(), R.color.zone4),
                    ContextCompat.getColor(requireContext(), R.color.zone5)
                )
                valueTextSize = 12f
            }
            val barData = BarData(dataSet)
            heartRateZoneChart.data = barData

            val avgHeartRate = viewModel.dailyAverageHeartRate.value ?: 0
            val zones = listOf(
                avgHeartRate.toDouble(),
                avgHeartRate + (avgHeartRate * 0.3),
                avgHeartRate + (avgHeartRate * 0.5),
                avgHeartRate + (avgHeartRate * 0.7),
                avgHeartRate + (avgHeartRate * 0.9)
            )

            val zoneLabels = listOf(
                "≤${zones[0].toInt()} BPM",
                "${zones[0].toInt()} ~ ${zones[1].toInt()} BPM",
                "${zones[1].toInt()} ~ ${zones[2].toInt()} BPM",
                "${zones[2].toInt()} ~ ${zones[3].toInt()} BPM",
                ">${zones[3].toInt()} BPM"
            )

            val xAxis = heartRateZoneChart.xAxis
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return zoneLabels.getOrNull(value.toInt()) ?: value.toString()
                }
            }
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawLabels(true)
            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)
            xAxis.textSize = 12f

            val leftAxis: YAxis = heartRateZoneChart.axisLeft
            leftAxis.setDrawGridLines(false)
            leftAxis.setDrawAxisLine(false)

            val rightAxis: YAxis = heartRateZoneChart.axisRight
            rightAxis.setDrawGridLines(false)
            rightAxis.setDrawAxisLine(false)
            rightAxis.setDrawLabels(false)

            heartRateZoneChart.description.isEnabled = false
            heartRateZoneChart.legend.isEnabled = false
            heartRateZoneChart.invalidate()
        }

        chartViewModel.segmentHeartRateEntries.observe(viewLifecycleOwner) { entries ->
            Log.d(TAG, "Updating segment heart rate chart with entries: $entries")
            val dataSet = LineDataSet(entries, "Segment Heart Rate").apply {
                color = ContextCompat.getColor(requireContext(), R.color.colorAccent)
                valueTextSize = 12f
                setDrawCircles(false)
                setDrawValues(false)
            }
            val lineData = LineData(dataSet)
            segmentHeartRateChart.data = lineData
            segmentHeartRateChart.description.isEnabled = false
            segmentHeartRateChart.legend.isEnabled = false

            val xAxis = segmentHeartRateChart.xAxis
            xAxis.setDrawGridLines(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawLabels(true)
            xAxis.setDrawAxisLine(false)
            xAxis.textSize = 12f
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()} m"
                }
            }

            val leftAxis = segmentHeartRateChart.axisLeft
            leftAxis.setDrawGridLines(false)
            leftAxis.setDrawAxisLine(false)

            val rightAxis = segmentHeartRateChart.axisRight
            rightAxis.setDrawGridLines(false)
            rightAxis.setDrawAxisLine(false)
            rightAxis.setDrawLabels(false)

            segmentHeartRateChart.invalidate()
        }

        viewModel.isTracking.observe(viewLifecycleOwner) { isTracking ->
            if (isTracking) {
                startButton.text = "정지"
                startButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))
                val serviceIntent = Intent(requireContext(), ExerciseService::class.java)
                ContextCompat.startForegroundService(requireContext(), serviceIntent)
            } else {
                startButton.text = "시작"
                startButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                val serviceIntent = Intent(requireContext(), ExerciseService::class.java)
                requireContext().stopService(serviceIntent)
            }
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(locationReceiver, IntentFilter("com.example.avatar_crab.LOCATION_UPDATE"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(locationReceiver)
    }

    private fun updateHeartRate(heartRate: Int) {
        tvHeartRate.text = "심박수: $heartRate BPM"
        heartRateReadings.add(heartRate)
        viewModel.saveHeartRateData(heartRate)
        if (tracking) {
            val avgHeartRate = viewModel.dailyAverageHeartRate.value ?: 0
            chartViewModel.updateHeartRateZones(meterHeartRates, avgHeartRate)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        getLocation()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSION)
        } else {
            getLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            } else {
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLocation = LatLng(it.latitude, it.longitude)
                mMap.addMarker(MarkerOptions().position(currentLocation).title("현재 위치"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18f))
            }
        }
    }

    private fun startTracking() {
        tracking = true
        viewModel.startTracking()
        startTime = SystemClock.elapsedRealtime()
        totalDistance = 0f
        totalCalories = 0f
        pathPoints.clear()
        heartRateReadings.clear()
        meterHeartRates.clear()
        hundredMeterSegments.clear()

        chartViewModel.clearCharts()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)

        val serviceIntent = Intent(requireContext(), ExerciseService::class.java)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }

    private fun stopTracking() {
        tracking = false
        viewModel.stopTracking()

        val elapsedTime = SystemClock.elapsedRealtime() - startTime

        sensorManager.unregisterListener(this)

        fusedLocationClient.removeLocationUpdates(locationCallback)

        calculateSegmentData()

        val avgHeartRate = calculateAverageHeartRate()
        val minHeartRate = calculateMinHeartRate()
        val maxHeartRate = calculateMaxHeartRate()

        val segments = hundredMeterSegments.map {
            SegmentDataEntity(it.avgHeartRate, it.minHeartRate, it.maxHeartRate, it.latitude, it.longitude)
        }

        val intent = Intent(requireContext(), SegmentDetailActivity::class.java).apply {
            putExtra("avgHeartRate", avgHeartRate)
            putExtra("minHeartRate", minHeartRate)
            putExtra("maxHeartRate", maxHeartRate)
            putParcelableArrayListExtra("segments", ArrayList(segments))
            putParcelableArrayListExtra("pathPoints", ArrayList(pathPoints.map { LatLngEntity(it.latitude, it.longitude) }))
        }
        startActivity(intent)

        saveActivity(elapsedTime)

        Toast.makeText(requireContext(), "활동이 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun calculateAverageHeartRate(): Float {
        return if (heartRateReadings.isNotEmpty()) heartRateReadings.average().toFloat() else 0f
    }

    private fun calculateMinHeartRate(): Float {
        return heartRateReadings.minOrNull()?.toFloat() ?: 0f
    }

    private fun calculateMaxHeartRate(): Float {
        return heartRateReadings.maxOrNull()?.toFloat() ?: 0f
    }

    private fun updateUI(location: Location) {
        if (tracking) {
            val currentLocation = LatLng(location.latitude, location.longitude)
            pathPoints.add(currentLocation)
            mMap.clear()
            mMap.addPolyline(
                PolylineOptions()
                    .addAll(pathPoints)
                    .width(30f)
                    .color(ContextCompat.getColor(requireContext(), R.color.colorAccent))
            )

            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))

            if (pathPoints.size > 1) {
                val previousLocation = pathPoints[pathPoints.size - 2]
                val results = FloatArray(1)
                Location.distanceBetween(
                    previousLocation.latitude, previousLocation.longitude,
                    currentLocation.latitude, currentLocation.longitude,
                    results
                )
                totalDistance += results[0]
                meterHeartRates.add(SegmentDataEntity(
                    avgHeartRate = heartRateReadings.lastOrNull()?.toFloat() ?: 0f,
                    minHeartRate = heartRateReadings.minOrNull()?.toFloat() ?: 0f,
                    maxHeartRate = heartRateReadings.maxOrNull()?.toFloat() ?: 0f,
                    latitude = currentLocation.latitude.toFloat(),
                    longitude = currentLocation.longitude.toFloat()
                ))
            }

            if (totalDistance >= 1000) {
                tvKm.text = "${"%.2f".format(totalDistance / 1000f)} km"
            } else {
                tvKm.text = "${totalDistance.roundToInt()} m"
            }

            totalCalories = totalDistance * 0.06f
            tvCalories.text = "칼로리: ${totalCalories.roundToInt()}"

            if (pathPoints.size == 1) {
                mMap.addMarker(
                    MarkerOptions()
                        .position(currentLocation)
                        .title("시작 지점")
                        .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.flag_start))
                )
            }
            mMap.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title("현재 위치")
                    .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.person_end))
            )

            chartViewModel.updateSegmentHeartRateChart(pathPoints, heartRateReadings)
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable?.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && tracking) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER || event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                // 센서 데이터 처리
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun calculateSegmentData() {
        var segmentDistance = 0f
        var segmentHeartRates = mutableListOf<Int>()

        pathPoints.forEachIndexed { index, latLng ->
            if (index > 0) {
                val previousLatLng = pathPoints[index - 1]
                val results = FloatArray(1)
                Location.distanceBetween(
                    previousLatLng.latitude, previousLatLng.longitude,
                    latLng.latitude, latLng.longitude,
                    results
                )
                segmentDistance += results[0]

                if (segmentDistance >= 100) {
                    if (segmentHeartRates.isNotEmpty()) {
                        val averageHeartRate = segmentHeartRates.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0
                        val minHeartRate = segmentHeartRates.minOrNull() ?: 0
                        val maxHeartRate = segmentHeartRates.maxOrNull() ?: 0
                        hundredMeterSegments.add(SegmentDataEntity(
                            avgHeartRate = averageHeartRate.toFloat(),
                            minHeartRate = minHeartRate.toFloat(),
                            maxHeartRate = maxHeartRate.toFloat(),
                            latitude = latLng.latitude.toFloat(),
                            longitude = latLng.longitude.toFloat()
                        ))
                    }
                    segmentDistance = 0f
                    segmentHeartRates.clear()
                }
            }

            if (index < heartRateReadings.size) {
                segmentHeartRates.add(heartRateReadings[index])
            }
        }
    }

    private fun saveActivity(elapsedTime: Long) {
        val heartRateText = tvHeartRate.text.toString().removePrefix("심박수: ").removeSuffix(" BPM")
        val heartRateValue = if (heartRateText.isBlank()) 0 else heartRateText.toInt()

        val activityData = ActivityData(
            bpm = heartRateValue,
            tag = "운동",
            timestamp = System.currentTimeMillis()
        )

        viewModel.addActivityData(activityData)

        val email = viewModel.userEmail.value ?: "unknown"

        val record = ExerciseRecord(
            id = 0,
            distance = totalDistance.toDouble(),
            elapsedTime = elapsedTime,
            calories = totalCalories.toDouble(),
            avgPace = if (totalDistance > 0) (elapsedTime / 1000f) / totalDistance.toDouble() else 0.0,
            date = System.currentTimeMillis(),
            segments = hundredMeterSegments,
            pathPoints = pathPoints,
            email = email
        )

        viewModel.saveExerciseRecord(record)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }
}
