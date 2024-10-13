package com.example.avatar_crab.presentation.data

import com.google.gson.annotations.SerializedName

data class HeartInfo(
    val rest: HeartRateRange,
    val active: HeartRateRange,
    val exercise: HeartRateRange,
    @SerializedName("periodic_data") val periodicData: PeriodicHeartData? // JSON의 periodic_data를 매핑합니다.
)

data class HeartRateRange(
    val max: Int,
    val min: Int
)

data class PeriodicHeartData(
    val daily: List<HeartDataPoint>,
    val weekly: List<HeartDataPoint>,
    val monthly: List<HeartDataPoint>,
    @SerializedName("six_months") val sixMonths: List<HeartDataPoint>, // JSON의 six_months를 매핑합니다.
    val yearly: List<HeartDataPoint>
)

data class HeartDataPoint(
    val label: String,
    val values: Int
)
