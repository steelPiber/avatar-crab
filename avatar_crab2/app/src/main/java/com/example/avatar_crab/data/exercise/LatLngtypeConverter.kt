package com.example.avatar_crab.data.exercise

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LatLngTypeConverter {

    @TypeConverter
    fun fromLatLngList(latLngList: List<LatLngEntity>): String {
        val gson = Gson()
        val type = object : TypeToken<List<LatLngEntity>>() {}.type
        return gson.toJson(latLngList, type)
    }

    @TypeConverter
    fun toLatLngList(latLngString: String): List<LatLngEntity> {
        val gson = Gson()
        val type = object : TypeToken<List<LatLngEntity>>() {}.type
        return gson.fromJson(latLngString, type)
    }
}


