package com.example.avatar_crab.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.avatar_crab.data.challenge.ChallengeRepository

class MainViewModelFactory(
    private val challengeRepository: ChallengeRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(challengeRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
