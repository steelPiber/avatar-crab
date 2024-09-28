package com.example.avatar_crab.data.exercise

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "exercise_records")
data class ExerciseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val distance: Double,
    val elapsedTime: Long,
    val calories: Double,
    val avgPace: Double,
    val date: Long,
    @TypeConverters(SegmentDataConverter::class)
    val segments: List<SegmentDataEntity>,
    @TypeConverters(LatLngEntityConverter::class)
    val pathPoints: List<LatLngEntity>,
    val email: String
)
