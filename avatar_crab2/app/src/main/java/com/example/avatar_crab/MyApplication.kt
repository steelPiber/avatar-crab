package com.example.avatar_crab

import android.app.Application
import com.example.avatar_crab.data.AppDatabase
import com.example.avatar_crab.data.challenge.ChallengeRepository
import com.naver.maps.map.NaverMapSdk

class MyApplication : Application() {

    lateinit var challengeRepository: ChallengeRepository

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        val challengeDao = database.challengeDao()
        val exerciseRecordDao = database.exerciseRecordDao()
        challengeRepository = ChallengeRepository(challengeDao, exerciseRecordDao)
        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NaverCloudPlatformClient("c1ka4vgyt7")    }
}
