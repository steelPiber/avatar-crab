package com.example.avatar_crab.presentation.exercise

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.avatar_crab.data.exercise.SegmentDataEntity
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.google.android.gms.maps.model.LatLng

class ChartViewModel : ViewModel() {

    private val _heartRateZoneEntries = MutableLiveData<List<BarEntry>>()
    val heartRateZoneEntries: LiveData<List<BarEntry>> get() = _heartRateZoneEntries

    private val _segmentHeartRateEntries = MutableLiveData<List<Entry>>()
    val segmentHeartRateEntries: LiveData<List<Entry>> get() = _segmentHeartRateEntries

    fun clearCharts() {
        _heartRateZoneEntries.value = emptyList()
        _segmentHeartRateEntries.value = emptyList()
    }

    fun updateHeartRateZones(meterHeartRates: MutableList<SegmentDataEntity>, avgHeartRate: Int) {
        val zoneCounts = IntArray(5)
        val zoneThresholds = listOf(
            avgHeartRate.toDouble(),
            avgHeartRate + (avgHeartRate * 0.3),
            avgHeartRate + (avgHeartRate * 0.5),
            avgHeartRate + (avgHeartRate * 0.7),
            avgHeartRate + (avgHeartRate * 0.9)
        )

        meterHeartRates.forEach { data ->
            val heartRate = data.avgHeartRate
            when {
                heartRate <= zoneThresholds[0] -> zoneCounts[0]++
                heartRate <= zoneThresholds[1] -> zoneCounts[1]++
                heartRate <= zoneThresholds[2] -> zoneCounts[2]++
                heartRate <= zoneThresholds[3] -> zoneCounts[3]++
                else -> zoneCounts[4]++
            }
        }

        val entries = zoneCounts.mapIndexed { index, count -> BarEntry(index.toFloat(), count.toFloat()) }
        _heartRateZoneEntries.value = entries
    }

    fun updateSegmentHeartRateChart(pathPoints: List<LatLng>, heartRates: List<Int>) {
        var totalDistance = 0f
        val entries = mutableListOf<Entry>()

        val minSize = minOf(pathPoints.size, heartRates.size)

        for (i in 1 until minSize) {
            val previousLatLng = pathPoints[i - 1]
            val currentLatLng = pathPoints[i]

            val results = FloatArray(1)
            Location.distanceBetween(
                previousLatLng.latitude, previousLatLng.longitude,
                currentLatLng.latitude, currentLatLng.longitude,
                results
            )
            totalDistance += results[0]

            entries.add(Entry(totalDistance, heartRates[i].toFloat()))
        }

        _segmentHeartRateEntries.value = entries
    }
}

