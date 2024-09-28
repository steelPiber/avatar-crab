package com.example.avatar_crab.presentation

import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.wearable.*

class WearableDataLayerService : WearableListenerService() {
    private val TAG = "WearableDataLayerService"

    // WearableDataLayerService.kt의 onDataChanged 함수 수정
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == "/data_path") {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    val message = dataMap.getString("data")
                    Log.d(TAG, "Data received: $message")
                }
            }
        }
    }

}
