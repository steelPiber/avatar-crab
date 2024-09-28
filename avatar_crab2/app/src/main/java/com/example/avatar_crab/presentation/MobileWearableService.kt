package com.example.avatar_crab.presentation

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class MobileWearableService : Service(), DataClient.OnDataChangedListener {

    override fun onCreate() {
        super.onCreate()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == "/heart_rate") {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    val bpm = dataMap.getString("bpm")
                    val tag = dataMap.getString("tag")
                    val timestamp = dataMap.getString("timestamp")

                    Log.d("MobileWearableService", "Data received: $bpm BPM, Tag: $tag at $timestamp")

                    // 데이터를 MainActivity로 전달
                    val intent = Intent("com.example.avatar_crab.HEART_RATE_UPDATE")
                    intent.putExtra("bpm", bpm)
                    intent.putExtra("tag", tag)
                    intent.putExtra("timestamp", timestamp)
                    sendBroadcast(intent)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
