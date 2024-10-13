// HeartInfo 데이터 클래스 정의
package com.example.avatar_crab.presentation.data

data class HeartInfo(
    val rest: HeartRateRange,
    val active: HeartRateRange,
    val exercise: HeartRateRange,
    val periodicData: PeriodicHeartData
)

data class HeartRateRange(
    val max: Int,
    val min: Int
)

data class PeriodicHeartData(
    val daily: List<HeartDataPoint>,
    val weekly: List<HeartDataPoint>,
    val monthly: List<HeartDataPoint>,
    val sixMonths: List<HeartDataPoint>,
    val yearly: List<HeartDataPoint>
)

data class HeartDataPoint(
    val label: String,
    val values: Int
)