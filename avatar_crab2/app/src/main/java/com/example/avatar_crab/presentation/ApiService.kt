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

    @POST("/api/record")
    fun sendRecord(@Body record: ExerciseRecord): Call<Void>

    @GET("/api/records/{email}")
    fun getRecords(@Path("email") email: String): Call<List<ExerciseRecordEntity>>

    @GET("/api/records/{email}/{id}/segments")
    fun getSegments(@Path("email") email: String, @Path("id") recordId: String): Call<List<SegmentDataEntity>>

    // 서버로 유저 정보를 보내는 POST 요청
    @POST("/api/userinfo")
    fun sendUserInfo(@Body userInfo: UserInfo): Call<Void>

    // 이메일을 사용하여 유저 정보를 가져오는 GET 요청
    @GET("/api/userinfo/{email}")
    fun getUserInfo(@Path("email") email: String): Call<UserInfo>
}

