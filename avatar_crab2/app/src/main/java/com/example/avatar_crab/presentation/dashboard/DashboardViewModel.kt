package com.example.avatar_crab.presentation.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.avatar_crab.data.ActivityData
import com.example.avatar_crab.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val activityDataDao = AppDatabase.getDatabase(application).activityDataDao()

    private val _activityData = MutableLiveData<List<ActivityData>>()
    val activityData: LiveData<List<ActivityData>> get() = _activityData

    fun loadTodayActivityData() {
        viewModelScope.launch(Dispatchers.IO) {
            val todayData = withContext(Dispatchers.IO) {
                val startOfDay = getStartOfDayTimestamp()
                val endOfDay = getEndOfDayTimestamp()
                activityDataDao.getActivityDataBetween(startOfDay, endOfDay)
            }
            _activityData.postValue(todayData)
        }
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun updateChallengesWithActivityData(activityData: List<ActivityData>) {
        // Update challenges with activity data
    }

    fun clearOldData(timestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            activityDataDao.deleteOldData(timestamp)
        }
    }
}
