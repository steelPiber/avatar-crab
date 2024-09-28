package com.example.avatar_crab.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class HeartRateViewModel(application: Application) : AndroidViewModel(application) {

    private val _heartRate = MutableLiveData<String>()
    val heartRate: LiveData<String> get() = _heartRate

    private val _activityTag = MutableLiveData<String>()
    val activityTag: LiveData<String> get() = _activityTag

    fun updateHeartRate(bpm: String) {
        _heartRate.postValue(bpm)
    }

    fun updateTag(tag: String) {
        _activityTag.postValue(tag)
    }
}
