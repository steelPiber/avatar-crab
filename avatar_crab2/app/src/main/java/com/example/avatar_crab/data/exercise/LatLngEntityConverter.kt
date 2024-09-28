package com.example.avatar_crab.data.exercise

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LatLngEntityConverter {
    @TypeConverter
    fun fromLatLngEntityList(value: List<LatLngEntity>): String {
        val gson = Gson()
        val type = object : TypeToken<List<LatLngEntity>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toLatLngEntityList(value: String): List<LatLngEntity> {
        val gson = Gson()
        val type = object : TypeToken<List<LatLngEntity>>() {}.type
        return gson.fromJson(value, type)
    }
}
