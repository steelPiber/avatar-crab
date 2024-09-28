package com.example.avatar_crab.presentation

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.net.ConnectivityManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.avatar_crab.R
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class HeartRateService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private lateinit var viewModel: HeartRateViewModel
    private val viewModelStore = ViewModelStore()
    private val handler = Handler(Looper.getMainLooper())

    private val accelerationValues = FloatArray(3)
    private val gyroscopeValues = FloatArray(3)
    private val accelerationBuffer = mutableListOf<Float>()
    private val gyroscopeBuffer = mutableListOf<Float>()

    private var lastBpm: Int? = null
    private var lastTag: String? = null
    private var lastActiveTime: Long = 0
    private var lastBpmSentTime: Long = 0
    private val tagsCollected = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        viewModel = ViewModelProvider(viewModelStore, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(HeartRateViewModel::class.java)

        startForegroundService()

        registerSensors()
        scheduleNextMeasurement()
        scheduleBpmUpdate()
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Heart Rate Monitor")
            .setContentText("Monitoring heart rate...")
            .setSmallIcon(R.drawable.ic_heart)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Heart Rate Monitor Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        startForeground(1, notification)
    }

    private fun registerSensors() {
        heartRateSensor?.also { sensor ->
            sensorManager.registerListener(this@HeartRateService, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometerSensor?.also { sensor ->
            sensorManager.registerListener(this@HeartRateService, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscopeSensor?.also { sensor ->
            sensorManager.registerListener(this@HeartRateService, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleNextMeasurement()
        scheduleBpmUpdate()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        viewModelStore.clear()
        handler.removeCallbacks(measurementRunnable)
        handler.removeCallbacks(bpmRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val measurementRunnable = object : Runnable {
        override fun run() {
            if (!isCharging()) {
                collectSensorData()
                if (accelerationBuffer.size >= 5 && gyroscopeBuffer.size >= 5) {
                    val tag = determineActivityTag()
                    tagsCollected.add(tag)
                    Log.d("HeartRateService", "5초 동안 측정된 태그: ${tagsCollected.joinToString()}")

                    if (tagsCollected.size == 5) {
                        determineMostFrequentTag()
                        tagsCollected.clear()
                    }

                    accelerationBuffer.clear()
                    gyroscopeBuffer.clear()
                }
            }
            handler.postDelayed(this, 1000L)  // 1초마다 실행
        }
    }

    private val bpmRunnable = object : Runnable {
        override fun run() {
            if (!isCharging()) {
                sendHeartRateDataIfDue()
            }
            val delay = if (isScreenOnOrAppRunning()) 1000L else 3000L
            handler.postDelayed(this, delay)
        }
    }

    private fun scheduleNextMeasurement() {
        handler.post(measurementRunnable)
    }

    private fun scheduleBpmUpdate() {
        handler.post(bpmRunnable)
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    private fun isScreenOnOrAppRunning(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
        return isScreenOn || isAppRunning()
    }

    private fun isAppRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName
        return appProcesses.any { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && it.processName == packageName }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_HEART_RATE -> {
                    val bpm = event.values[0].toInt()
                    if (bpm > 0) {
                        viewModel.updateHeartRate(bpm.toString())
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    val magnitude = sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2])
                    accelerationBuffer.add(magnitude)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    val magnitude = sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2])
                    gyroscopeBuffer.add(magnitude)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun collectSensorData() {
        // 이미 센서 데이터가 버퍼에 추가되는 중이므로 이 함수에서는 특별한 작업이 필요 없습니다.
    }

    private fun determineActivityTag(): String {
        val avgAccelerationMagnitude = accelerationBuffer.average().toFloat()
        val avgGyroscopeMagnitude = gyroscopeBuffer.average().toFloat()
        val bpm = viewModel.heartRate.value?.toIntOrNull() ?: 0

        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val isNight = hour >= 22 || hour < 6

        val sharedPreferences = getSharedPreferences("HeartRateServicePrefs", Context.MODE_PRIVATE)
        val lastMovementTime = sharedPreferences.getLong("lastMovementTime", System.currentTimeMillis())
        val currentTimeMillis = System.currentTimeMillis()
        val hasMinimalMovementFor30Minutes = (currentTimeMillis - lastMovementTime) >= 30 * 60 * 1000

        if (bpm <= 70 && (hasMinimalMovementFor30Minutes || isNight) && avgAccelerationMagnitude < 0.5) {
            return "sleep"
        }

        sharedPreferences.edit().putLong("lastMovementTime", currentTimeMillis).apply()

        return when {
            avgGyroscopeMagnitude >= 3.0 && avgAccelerationMagnitude >= 3.0 -> {
                lastActiveTime = System.currentTimeMillis()
                "exercise"
            }
            avgGyroscopeMagnitude >= 2.0 && avgAccelerationMagnitude >= 2.0 -> {
                lastActiveTime = System.currentTimeMillis()
                "active"
            }
            avgGyroscopeMagnitude >= 1.0 && avgAccelerationMagnitude >= 1.0 -> {
                if (isWalking(avgAccelerationMagnitude, avgGyroscopeMagnitude)) {
                    "active"
                } else {
                    "rest"
                }
            }
            else -> "rest"
        }
    }

    private fun isWalking(accelerationMagnitude: Float, gyroscopeMagnitude: Float): Boolean {
        return accelerationMagnitude in 1.0..2.0 && gyroscopeMagnitude in 1.0..2.0
    }

    private fun determineMostFrequentTag() {
        val tagCount = tagsCollected.groupingBy { it }.eachCount()
        if (tagCount.isNotEmpty()) {
            val mostFrequentTag = tagCount.maxByOrNull { it.value }?.key ?: "rest"
            Log.d("HeartRateService", "5번 측정된 태그: ${tagsCollected.joinToString()}")
            Log.d("HeartRateService", "가장 많이 발생한 태그: $mostFrequentTag")
            lastTag = mostFrequentTag
            sendHeartRateDataIfDue()
        }
    }

    private fun sendHeartRateDataIfDue() {
        val bpm = viewModel.heartRate.value?.toIntOrNull() ?: 0
        val currentTimeMillis = System.currentTimeMillis()
        if (bpm > 0 && currentTimeMillis - lastBpmSentTime >= 1000) {
            sendHeartRateData(bpm.toString(), lastTag ?: "rest")
            broadcastHeartRateData(bpm.toString(), lastTag ?: "rest")
            lastBpmSentTime = currentTimeMillis
        }
    }

    private fun averageMagnitude(buffer: List<FloatArray>): Float {
        if (buffer.isEmpty()) return 0f

        val sum = buffer.fold(FloatArray(3)) { acc, values ->
            acc[0] += values[0]
            acc[1] += values[1]
            acc[2] += values[2]
            acc
        }

        val size = buffer.size.toFloat()
        return sqrt((sum[0] / size) * (sum[0] / size) + (sum[1] / size) * (sum[1] / size) + (sum[2] / size) * (sum[2] / size))
    }

    private fun broadcastHeartRateData(bpm: String, tag: String) {
        val intent = Intent("com.example.avatar_crab.HEART_RATE_UPDATE")
        intent.putExtra("bpm", bpm)
        intent.putExtra("tag", tag)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    @SuppressLint("ServiceCast")
    private fun sendHeartRateData(bpm: String, tag: String) {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo?.isConnected ?: false

        if (isConnected) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val putDataMapRequest = PutDataMapRequest.create("/heart_rate").apply {
                dataMap.putString("bpm", bpm)
                dataMap.putString("timestamp", timestamp)
                dataMap.putString("tag", tag)
            }
            val request = putDataMapRequest.asPutDataRequest().setUrgent()

            val dataClient = Wearable.getDataClient(this)
            val putDataTask = dataClient.putDataItem(request)

            putDataTask.addOnSuccessListener {
                Log.d("HeartRateService", "Heart rate data sent successfully: $bpm BPM, Tag: $tag")
            }

            putDataTask.addOnFailureListener { e ->
                Log.e("HeartRateService", "Failed to send heart rate data", e)
            }
        }
    }

    private fun isCharging(): Boolean {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.isCharging
    }

    companion object {
        const val CHANNEL_ID = "HeartRateServiceChannel"
    }
}
