package com.example.avatar_crab.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val _notifications = MutableLiveData<List<NotificationData>>()
    val notifications: LiveData<List<NotificationData>> = _notifications

    init {
        _notifications.value = mutableListOf()
    }

    fun addNotification(notification: NotificationData) {
        val updatedList = _notifications.value?.toMutableList() ?: mutableListOf()
        updatedList.add(notification)
        _notifications.value = updatedList
    }
}
