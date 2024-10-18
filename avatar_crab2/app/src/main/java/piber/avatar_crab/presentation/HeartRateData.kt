package piber.avatar_crab.presentation

data class HeartRateData(
    val bpm: String?,           // 심박수
    val tag: String?,           // 태그
    val timestamp: String?,     // 타임스탬프
    val email: String?,         // 이메일
    val latitude: Double?,      // 위도
    val longitude: Double?      // 경도
)