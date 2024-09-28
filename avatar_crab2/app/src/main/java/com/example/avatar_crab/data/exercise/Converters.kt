// Converters.kt
package com.example.avatar_crab.data.exercise

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromSegmentDataList(segmentDataList: List<SegmentDataEntity>): String {
        return Gson().toJson(segmentDataList)
    }

    @TypeConverter
    fun toSegmentDataList(data: String): List<SegmentDataEntity> {
        val listType = object : TypeToken<List<SegmentDataEntity>>() {}.type
        return Gson().fromJson(data, listType)
    }

    @TypeConverter
    fun fromLatLngList(latLngList: List<LatLngEntity>): String {
        return Gson().toJson(latLngList)
    }

    @TypeConverter
    fun toLatLngList(data: String): List<LatLngEntity> {
        val listType = object : TypeToken<List<LatLngEntity>>() {}.type
        return Gson().fromJson(data, listType)
    }
}
