package com.example.avatar_crab.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HeartRateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var lastHeartRate: Int = 0

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
            heartRateSensor?.also { sensor ->
                sensorManager.registerListener(this@HeartRateWorker, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                delayForSensorData()
            }
            Result.success()
        }
    }

    private suspend fun delayForSensorData() {
        withContext(Dispatchers.IO) {
            // 센서 데이터 수집을 위한 딜레이 (10초 대기)
            kotlinx.coroutines.delay(10000)
            sensorManager.unregisterListener(this@HeartRateWorker)
            sendHeartRateData(lastHeartRate.toString())
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            lastHeartRate = event.values[0].toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun sendHeartRateData(bpm: String) {
        // 서버로 데이터 전송 로직 구현
    }
}
