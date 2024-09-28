package com.example.avatar_crab.presentation.exercise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentHeartRate = MutableLiveData<Int>()
    val currentHeartRate: LiveData<Int> get() = _currentHeartRate

    fun setCurrentHeartRate(heartRate: Int) {
        _currentHeartRate.value = heartRate
    }
}
