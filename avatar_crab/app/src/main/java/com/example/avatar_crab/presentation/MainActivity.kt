package com.example.avatar_crab.presentation

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HeartRateViewModel
    override val viewModelStore = ViewModelStore()
    private val handler = Handler()
    private lateinit var updateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(viewModelStore, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(HeartRateViewModel::class.java)

        setContent {
            WearApp(viewModel)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 1)
        } else {
            startHeartRateService()
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(heartRateReceiver, IntentFilter("com.example.avatar_crab.HEART_RATE_UPDATE"))

        updateRunnable = object : Runnable {
            override fun run() {
                val intent = Intent(this@MainActivity, HeartRateService::class.java)
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                    powerManager.isInteractive
                } else {
                    @Suppress("DEPRECATION")
                    powerManager.isScreenOn
                }
                intent.putExtra("isForeground", isScreenOn)
                startService(intent)
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(updateRunnable)
    }

    private fun startHeartRateService() {
        val serviceIntent = Intent(this, HeartRateService::class.java)
        serviceIntent.putExtra("isForeground", true)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startHeartRateService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(heartRateReceiver)
        handler.removeCallbacks(updateRunnable)
    }

    private val heartRateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val bpm = intent?.getStringExtra("bpm") ?: "0"
            val tag = intent?.getStringExtra("tag") ?: "unknown"
            viewModel.updateHeartRate(bpm)
            viewModel.updateTag(tag)
        }
    }

    @Composable
    fun WearApp(viewModel: HeartRateViewModel) {
        val heartRate by viewModel.heartRate.observeAsState("0")
        val activityTag by viewModel.activityTag.observeAsState("unknown")

        MaterialTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Heart Rate: $heartRate BPM",
                    color = Color.White
                )
                Text(
                    text = "Activity: $activityTag",
                    color = Color.White
                )
            }
        }
    }
}
