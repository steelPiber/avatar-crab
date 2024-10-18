package piber.avatar_crab.presentation
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import piber.avatar_crab.data.challenge.ChallengeRepository

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
