package com.example.avatar_crab.data.exercise

import com.google.android.gms.maps.model.LatLng

data class ExerciseRecord(
    val id: Int,
    val distance: Double,
    val elapsedTime: Long,
    val calories: Double,
    val avgPace: Double,
    val date: Long,
    val segments: MutableList<SegmentDataEntity>,
    val pathPoints: List<LatLng>,
    val email: String
)
