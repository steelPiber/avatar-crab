package com.example.avatar_crab.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_data")
data class ActivityData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bpm: Int,
    val tag: String,
    val timestamp: Long,
    val distance: Float = 0f, // distance 필드 추가
    val calories: Float = 0f  // calories 필드 추가
)
