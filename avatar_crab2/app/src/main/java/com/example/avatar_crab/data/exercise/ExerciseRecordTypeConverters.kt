package com.example.avatar_crab.data.exercise

import androidx.room.TypeConverter
import com.example.avatar_crab.data.exercise.SegmentDataEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ExerciseRecordTypeConverters {
    @TypeConverter
    @JvmStatic
    fun fromSegments(value: List<SegmentDataEntity>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    @JvmStatic
    fun toSegments(value: String): List<SegmentDataEntity> {
        val listType = object : TypeToken<List<SegmentDataEntity>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
