package com.example.avatar_crab.presentation.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)

    private val _ecgEnabled = MutableLiveData<Boolean>()
    val ecgEnabled: LiveData<Boolean> get() = _ecgEnabled

    private val _bradycardiaEnabled = MutableLiveData<Boolean>()
    val bradycardiaEnabled: LiveData<Boolean> get() = _bradycardiaEnabled

    private val _tachycardiaEnabled = MutableLiveData<Boolean>()
    val tachycardiaEnabled: LiveData<Boolean> get() = _tachycardiaEnabled

    init {
        _ecgEnabled.value = sharedPreferences.getBoolean("ecgEnabled", false)
        _bradycardiaEnabled.value = sharedPreferences.getBoolean("bradycardiaEnabled", false)
        _tachycardiaEnabled.value = sharedPreferences.getBoolean("tachycardiaEnabled", false)
    }

    fun setEcgEnabled(enabled: Boolean) {
        _ecgEnabled.value = enabled
        sharedPreferences.edit().putBoolean("ecgEnabled", enabled).apply()
    }

    fun setBradycardiaEnabled(enabled: Boolean) {
        _bradycardiaEnabled.value = enabled
        sharedPreferences.edit().putBoolean("bradycardiaEnabled", enabled).apply()
    }

    fun setTachycardiaEnabled(enabled: Boolean) {
        _tachycardiaEnabled.value = enabled
        sharedPreferences.edit().putBoolean("tachycardiaEnabled", enabled).apply()
    }
}
