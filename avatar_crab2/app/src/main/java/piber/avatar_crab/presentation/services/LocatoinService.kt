package piber.avatar_crab.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import piber.avatar_crab.R

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()

        // Initialize FusedLocationProviderClient for location updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android S (API Level 31) 이상에서는 LocationRequest.Builder를 사용
            locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000 // 10초 간격으로 위치 업데이트
            ).apply {
                setMinUpdateIntervalMillis(5000) // 최소 업데이트 간격 5초
                setWaitForAccurateLocation(true) // 더 정확한 위치를 기다림
            }.build()
        } else {
            // Android S 이하의 버전에서는 기존의 LocationRequest 사용
            locationRequest = LocationRequest.create().apply {
                interval = 10000 // 10초 간격으로 위치 업데이트
                fastestInterval = 5000 // 최소 업데이트 간격 5초
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        }




        // Create a LocationCallback to handle location updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location: Location? = locationResult.lastLocation
                if (location != null) {
                    Log.d("LocationService", "Location received: ${location.latitude}, ${location.longitude}")
                    // Here, you can send the location to a server or use it within the app
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the service in the foreground
        startForeground(1, createNotification())

        // Start requesting location updates
        requestLocationUpdates()

        return START_STICKY
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // 권한이 부여된 경우 위치 업데이트 요청
            if (::fusedLocationClient.isInitialized) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
            }
        } else {
            // 권한이 없는 경우 로그 출력
            Log.e("LocationService", "위치 권한이 부여되지 않았습니다.")
        }
    }

    // Create a persistent notification for the foreground service
    private fun createNotification(): Notification {
        val notificationChannelId = "location_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId,
                "Location Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
        return notificationBuilder.setOngoing(true)
            .setContentTitle("Location Service")
            .setContentText("Receiving location updates")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
