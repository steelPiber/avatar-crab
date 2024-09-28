package com.example.avatar_crab.presentation.exercise

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.avatar_crab.R
import com.example.avatar_crab.data.AppDatabase
import com.example.avatar_crab.data.challenge.ChallengeRepository
import com.example.avatar_crab.presentation.MainViewModel
import com.example.avatar_crab.presentation.MainViewModelFactory
import com.google.android.gms.location.*

class ExerciseService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var viewModel: MainViewModel

    companion object {
        const val CHANNEL_ID = "ExerciseServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        val appDatabase = AppDatabase.getDatabase(application) // AppDatabase 인스턴스를 가져옵니다.
        val challengeRepository = ChallengeRepository(appDatabase.challengeDao(), appDatabase.exerciseRecordDao()) // ChallengeRepository 인스턴스를 생성합니다.
        val factory = MainViewModelFactory(challengeRepository, application)
        viewModel = ViewModelProvider(this as ViewModelStoreOwner, factory).get(MainViewModel::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val intent = Intent("com.example.avatar_crab.LOCATION_UPDATE")
                    intent.putExtra("location", location)
                    intent.putExtra("heartRate", getCurrentHeartRate()) // 심박수 데이터를 추가합니다.
                    sendBroadcast(intent)
                }
            }
        }
    }

    @SuppressLint("MissingPermission", "ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("운동 추적 중")
            .setContentText("운동이 종료될 때까지 위치를 추적합니다.")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification)

        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Exercise Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun getCurrentHeartRate(): Int {
        return viewModel.heartRate.value?.removeSuffix(" BPM")?.toIntOrNull() ?: 0
    }
}
