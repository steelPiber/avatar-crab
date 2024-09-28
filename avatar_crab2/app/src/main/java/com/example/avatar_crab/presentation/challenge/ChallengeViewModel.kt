package com.example.avatar_crab.presentation.challenge

import androidx.lifecycle.*
import com.example.avatar_crab.data.challenge.Challenge
import com.example.avatar_crab.data.challenge.ChallengeRepository
import com.example.avatar_crab.data.ActivityData
import com.example.avatar_crab.data.exercise.ExerciseRecordEntity
import kotlinx.coroutines.launch
import java.util.Calendar

class ChallengeViewModel(private val repository: ChallengeRepository) : ViewModel() {

    private val _challenges = MutableLiveData<List<Challenge>>()
    val challenges: LiveData<List<Challenge>> get() = _challenges

    fun getChallengesByDate(date: Long): LiveData<List<Challenge>> {
        return repository.getChallengesByDate(date)
    }

    fun loadChallengesForToday() {
        viewModelScope.launch {
            val todayDate = getTodayDate()
            val challenges = repository.getOrGenerateChallenges(todayDate)
            _challenges.postValue(challenges)
        }
    }

    fun loadChallengesForDate(date: Long) {
        viewModelScope.launch {
            val challenges = repository.getOrGenerateChallenges(date)
            _challenges.postValue(challenges)
        }
    }

    fun updateChallengesWithActivityData(activityData: List<ActivityData>, exerciseRecords: List<ExerciseRecordEntity>) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            repository.updateChallengesWithActivityData(activityData, exerciseRecords, calendar)
            loadChallengesForToday()
        }
    }

    private fun getTodayDate(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

class ChallengeViewModelFactory(private val repository: ChallengeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChallengeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChallengeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
