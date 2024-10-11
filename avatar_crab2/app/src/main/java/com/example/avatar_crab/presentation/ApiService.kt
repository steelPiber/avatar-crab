package com.example.avatar_crab.presentation

import com.example.avatar_crab.data.exercise.ExerciseRecord
import com.example.avatar_crab.data.exercise.ExerciseRecordEntity
import com.example.avatar_crab.data.exercise.SegmentDataEntity
import com.example.avatar_crab.presentation.data.MapPolygonData
import com.example.avatar_crab.presentation.data.UserInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("/data")
    fun sendHeartRateData(@Body heartRateData: HeartRateData): Call<Void>

    @POST("/record")
    fun sendRecord(@Body record: ExerciseRecord): Call<Void>

    @GET("/records/{email}")
    fun getRecords(@Path("email") email: String): Call<List<ExerciseRecordEntity>>

    @GET("/records/{email}/{id}/segments")
    fun getSegments(@Path("email") email: String, @Path("id") recordId: String): Call<List<SegmentDataEntity>>

    // 서버로 유저 정보를 보내는 POST 요청
    @POST("/userinfo")
    fun sendUserInfo(@Body userInfo: UserInfo): Call<Void>

    // 이메일을 사용하여 유저 정보를 가져오는 GET 요청
    @GET("/userinfo/{email}")
    fun getUserInfo(@Path("email") email: String): Call<UserInfo>

    @POST("/userupdate")
    fun updateUserInfo(@Body userInfo: UserInfo): Call<Void>

    @GET("/userinfo/{email}/exists")
    fun checkUserInfo(@Path("email") email: String): Call<Boolean>

    // 저장된 폴리곤 데이터 전송 (POST)
    @POST("/mapinfo")
    fun sendPolygonData(@Body polygonData: MapPolygonData): Call<ResponseBody>

    // 폴리곤 데이터 조회 (GET)
    @GET("/mapinfo")
    fun getPolygonData(@Query("email") email: String): Call<List<MapPolygonData>>

    // 폴리곤 데이터 수정 (PUT)
    @PUT("/mapinfo")
    fun updatePolygonData(@Body polygonData: MapPolygonData): Call<ResponseBody>

    // 폴리곤 데이터 삭제 (DELETE)
    @DELETE("/mapinfo")
    fun deletePolygonData(@Query("email") email: String, @Query("title") title: String): Call<ResponseBody>
}


