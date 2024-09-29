package com.example.avatar_crab.presentation

import com.example.avatar_crab.data.exercise.ExerciseRecord
import com.example.avatar_crab.data.exercise.ExerciseRecordEntity
import com.example.avatar_crab.data.exercise.SegmentDataEntity
import com.example.avatar_crab.presentation.data.UserInfo
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("/data")
    fun sendHeartRateData(@Body heartRateData: HeartRateData): Call<Void>
    // 심박수 데이터를 서버로 전송하는 API 요청

    @POST("/api/record")
    fun sendRecord(@Body record: ExerciseRecord): Call<Void>
    // 운동 기록 데이터를 서버로 전송하는 API 요청

    @GET("/api/records/{email}")
    fun getRecords(@Path("email") email: String): Call<List<ExerciseRecordEntity>>
    // 주어진 이메일에 해당하는 사용자의 운동 기록을 서버에서 조회하는 API 요청

    @GET("/api/records/{email}/{id}/segments")
    fun getSegments(@Path("email") email: String, @Path("id") recordId: String): Call<List<SegmentDataEntity>>
    // 주어진 이메일과 기록 ID에 해당하는 세그먼트 데이터를 서버에서 조회하는 API 요청

    @POST("/userinfo")
    fun sendUserInfo(@Body userInfo: UserInfo): Call<Void>
    // 사용자 신체 정보를 서버로 전송하여 저장하는 API 요청

    @GET("/userinfo/{email}")
    fun getUserInfo(@Path("email") email: String): Call<UserInfo>
    // 주어진 이메일에 해당하는 사용자의 신체 정보를 서버에서 조회하는 API 요청

    @GET("/userinfo/{email}/exists")
    fun checkUserInfo(@Path("email") email: String): Call<Boolean>
    // 주어진 이메일에 해당하는 사용자가 존재하는지 서버에서 확인하는 API 요청
    // 서버에서 해당 이메일이 UserInfo 테이블에 존재하면 true, 아니면 false 반환
}
