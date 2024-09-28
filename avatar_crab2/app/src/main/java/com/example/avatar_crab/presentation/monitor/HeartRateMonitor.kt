package com.example.avatar_crab.presentation.monitor


import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast

class HeartRateMonitor(private val context: Context) {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private var isFlashOn = false
    private var heartRate = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    init {
        // 카메라 매니저 초기화
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0] // 후면 카메라 ID 사용
    }

    // 플래시 토글
    private fun toggleFlashlight(turnOn: Boolean) {
        try {
            cameraManager.setTorchMode(cameraId, turnOn)
            isFlashOn = turnOn
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // 심박수 측정 시작
    fun startHeartRateMeasurement(heartRateTextView: TextView) {
        heartRate = 0
        toggleFlashlight(true)
        runnable = object : Runnable {
            override fun run() {
                heartRate += 1
                heartRateTextView.text = "심박수: $heartRate BPM"
                if (isFlashOn) {
                    handler.postDelayed(this, 1000) // 1초마다 심박수 증가
                }
            }
        }
        handler.post(runnable)
        Toast.makeText(context, "심박수 측정 시작", Toast.LENGTH_SHORT).show()
    }

    // 심박수 측정 중단
    fun stopHeartRateMeasurement() {
        toggleFlashlight(false)
        handler.removeCallbacks(runnable)
        Toast.makeText(context, "심박수 측정 중지", Toast.LENGTH_SHORT).show()
    }

    fun isMeasuring(): Boolean {
        return isFlashOn
    }
}