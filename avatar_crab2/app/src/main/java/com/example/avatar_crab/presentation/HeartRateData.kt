package com.example.avatar_crab.presentation

data class HeartRateData(
    val bpm: String?,           // 심박수
    val tag: String?,           // 태그
    val timestamp: String?,     // 타임스탬프
    val email: String?,         // 이메일
    val latitude: String?,      // 위도
    val longitude: String?      // 경도
)