package com.example.avatar_crab.presentation

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL_RECORDS = "http://202.31.246.30:10021"
    private const val BASE_URL_HEART_RATE = "http://202.31.246.30:13389"

    val recordsInstance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_RECORDS)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val heartRateInstance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_HEART_RATE)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}


