package com.example.avatar_crab.presentation
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.avatar_crab.data.challenge.ChallengeRepository
import com.example.avatar_crab.presentation.MainViewModel

class MainViewModelFactory(
    private val challengeRepository: ChallengeRepository,
    private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(challengeRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
