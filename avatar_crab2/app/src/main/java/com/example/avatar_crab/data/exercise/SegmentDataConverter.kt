package com.example.avatar_crab.data.exercise

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SegmentDataConverter {
    @TypeConverter
    fun fromSegmentDataList(segments: List<SegmentDataEntity>): String {
        return Gson().toJson(segments)
    }

    @TypeConverter
    fun toSegmentDataList(data: String): List<SegmentDataEntity> {
        val listType = object : TypeToken<List<SegmentDataEntity>>() {}.type
        return Gson().fromJson(data, listType)
    }
}
